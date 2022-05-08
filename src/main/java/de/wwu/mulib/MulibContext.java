package de.wwu.mulib;

import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.executors.*;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.solving.LabelUtility;
import de.wwu.mulib.solving.Labels;
import de.wwu.mulib.solving.Solvers;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.Conc;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.MulibTransformer;
import de.wwu.mulib.transformations.MulibValueTransformer;
import de.wwu.mulib.transformations.asm_transformations.AsmMulibTransformer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;

public class MulibContext {
    @SuppressWarnings("all")
    private final MulibConfig mulibConfig;
    @SuppressWarnings("all")
    private final MethodHandle methodHandle;
    private final MulibExecutorManager mulibExecutorManager;
    private final SolverManager solverManager;
    private final Function<SymbolicExecution, Object[]> argsSupplier;
    private final MulibTransformer mulibTransformer;
    private final MulibValueTransformer mulibValueTransformer;

    protected MulibContext(
            String methodName,
            Class<?> owningMethodClass,
            MulibConfig config,
            boolean transformationRequired,
            Class<?>[] argTypes,
            Object[] args) {
        Class<?> possiblyTransformedMethodClass;
        Object[] searchRegionArgs;
        Class<?>[] searchRegionArgTypes;
        if (argTypes == null) {
            argTypes = findMethodFittingToArgs(args, methodName, owningMethodClass);
        }
        if (transformationRequired) {
            this.mulibTransformer = new AsmMulibTransformer(config);
            this.mulibTransformer.transformAndLoadClasses(owningMethodClass);
            possiblyTransformedMethodClass = this.mulibTransformer.getTransformedClass(owningMethodClass);
            this.mulibValueTransformer = new MulibValueTransformer(config, mulibTransformer, true);
            searchRegionArgs = transformArguments(mulibValueTransformer, args);
            searchRegionArgTypes = transformArgumentTypes(mulibValueTransformer, argTypes);
        } else {
            this.mulibTransformer = null;
            this.mulibValueTransformer = new MulibValueTransformer(config, null, false);
            possiblyTransformedMethodClass = owningMethodClass;
            searchRegionArgs = args;
            searchRegionArgTypes = argTypes;
        }

        this.mulibConfig = config;
        this.solverManager = Solvers.getSolverManager(config);
        this.argsSupplier = (se) -> {
            Object[] arguments = new Object[searchRegionArgs.length];
            for (int i = 0; i < arguments.length; i++) {
                Object arg = searchRegionArgs[i];
                if (arg instanceof SymSprimitive) {
                    if (arg instanceof Sint) {
                        if (arg instanceof Sbyte) {
                            arg = se.symSbyte();
                        } else if (arg instanceof Sshort) {
                            arg = se.symSshort();
                        } else if (arg instanceof Sbool) {
                            arg = se.symSbool();
                        } else {
                            arg = se.symSint();
                        }
                    } else if (arg instanceof Sdouble) {
                        arg = se.symSdouble();
                    } else if (arg instanceof Sfloat) {
                        arg = se.symSfloat();
                    } else if (arg instanceof Slong) {
                        arg = se.symSlong();
                    } else {
                        throw new NotYetImplementedException();
                    }
                } else {
                    se.getMulibValueTransformer().copySearchRegionRepresentation(arg);
                }
                arguments[i] = arg;
            }
            return arguments;
        };

        try {
            Method method = possiblyTransformedMethodClass.getDeclaredMethod(methodName, searchRegionArgTypes);
            this.methodHandle = MethodHandles.lookup().unreflect(method);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new MulibRuntimeException(e);
        } catch (VerifyError t) {
            t.printStackTrace();
            throw new MulibRuntimeException(t);
        }
        SearchTree searchTree = new SearchTree(config, methodHandle, argsSupplier);
        ChoicePointFactory choicePointFactory = ChoicePointFactory.getInstance(config);
        ValueFactory valueFactory = ValueFactory.getInstance(config);
        CalculationFactory calculationFactory = CalculationFactory.getInstance(config);
        this.mulibExecutorManager = config.ADDITIONAL_PARALLEL_SEARCH_STRATEGIES.isEmpty() ?
                new SingleExecutorManager(
                        config,
                        searchTree,
                        choicePointFactory,
                        valueFactory,
                        calculationFactory,
                        mulibValueTransformer
                )
                :
                new MultiExecutorsManager(
                        config,
                        searchTree,
                        choicePointFactory,
                        valueFactory,
                        calculationFactory,
                        mulibValueTransformer
                );
    }

    private static Class<?>[] findMethodFittingToArgs(Object[] args, String methodName, Class<?> owningMethodClass) {
        Class<?>[] directTypesOfArgs = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                directTypesOfArgs[i] = null;
            } else {
                directTypesOfArgs[i] = args[i].getClass();
            }
        }

        Method[] candidates = Arrays.stream(owningMethodClass.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .toArray(Method[]::new);

        for (Method m : candidates) {
            if (m.getParameterTypes().length != args.length) {
                continue;
            }
            Class<?>[] potentialResult = new Class<?>[directTypesOfArgs.length];
            Class<?>[] paramTypes = m.getParameterTypes();
            boolean valid = true;
            for (int i = 0; i < directTypesOfArgs.length; i++) {
                if (directTypesOfArgs[i] == null) {
                    if (!paramTypes[i].isPrimitive()) {
                        potentialResult[i] = paramTypes[i];
                        continue;
                    } else {
                        valid = false;
                        break;
                    }
                }
                if (!paramTypes[i].isAssignableFrom(directTypesOfArgs[i])) {
                    if (paramTypes[i].isPrimitive() && isWrapperOfType(paramTypes[i], directTypesOfArgs[i])) {
                        continue;
                    }
                    valid = false;
                    break;
                }
            }

            if (valid) {
                return m.getParameterTypes();
            }
        }
        throw new MulibRuntimeException("Method that fits the argument types: " + Arrays.toString(directTypesOfArgs) +
                " while having the name " + methodName + " cannot be found in class " + owningMethodClass.getName());
    }

    private static boolean isWrapperOfType(Class<?> type, Class<?> checkIfWrapper) {
        Class<?> mulibWrapper;
        Class<?> javaWrapper;
        if (type == int.class) {
            mulibWrapper = Sint.class;
            javaWrapper = Integer.class;
        } else if (type == long.class) {
            mulibWrapper = Slong.class;
            javaWrapper = Long.class;
        } else if (type == double.class) {
            mulibWrapper = Sdouble.class;
            javaWrapper = Double.class;
        } else if (type == float.class) {
            mulibWrapper = Sfloat.class;
            javaWrapper = Float.class;
        } else if (type == short.class) {
            mulibWrapper = Sshort.class;
            javaWrapper = Short.class;
        } else if (type == byte.class) {
            mulibWrapper = Sbyte.class;
            javaWrapper = Byte.class;
        } else if (type == boolean.class) {
            mulibWrapper = Sbool.class;
            javaWrapper = Boolean.class;
        } else if (type == String.class) {
            mulibWrapper = String.class;
            javaWrapper = String.class;
        } else {
            throw new NotYetImplementedException();
        }
        // TODO Arrays
        return mulibWrapper.isAssignableFrom(checkIfWrapper) || javaWrapper == checkIfWrapper;
    }

    public synchronized List<PathSolution> getAllPathSolutions() {
        long startTime = System.nanoTime();
        List<PathSolution> result = mulibExecutorManager.getAllSolutions();
        long endTime = System.nanoTime();
        Mulib.log.log(Level.INFO, "Took " + (endTime - startTime) + "ns");
        return result;
    }

    public synchronized Optional<PathSolution> getPathSolution() {
        return mulibExecutorManager.getSolution();
    }

    public synchronized List<Solution> getAllSolutions(PathSolution pathSolution) {
        return getUpToNSolutions(pathSolution, Integer.MAX_VALUE);
    }

    private static Object[] transformArguments(
            MulibValueTransformer mulibValueTransformer,
            Object[] args) {
        Object[] result = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = mulibValueTransformer.transformValue(args[i]);
        }
        return result;
    }

    private static Class<?>[] transformArgumentTypes(
            MulibValueTransformer mulibValueTransformer,
            Class<?>[] argTypes) {
        Class<?>[] result = new Class[argTypes.length];
        for (int i = 0; i < argTypes.length; i++) {
            result[i] = mulibValueTransformer.transformType(argTypes[i]);
        }
        return result;
    }

    public synchronized List<Solution> getUpToNSolutions(PathSolution pathSolution, int N) {
        if (pathSolution.getCurrentlyInitializedSolutions().size() >= N) {
            return new ArrayList<>(pathSolution.getCurrentlyInitializedSolutions());
        }
        solverManager.backtrackAll();
        List<Constraint> constraintList = new ArrayList<>();
        constraintList.add(Sbool.TRUE);
        constraintList.addAll(Arrays.asList(pathSolution.getPathConstraints()));
        solverManager.addConstraintAfterNewBacktrackingPoint(And.newInstance(constraintList));
        solverManager.addArrayConstraints(pathSolution.getArrayConstraints());
        List<Solution> solutions = new ArrayList<>(pathSolution.getCurrentlyInitializedSolutions());
        while (solverManager.isSatisfiable() && solutions.size() < N) {
            Solution latestSolution = pathSolution.getLatestSolution();
            Constraint[] latestSolutionConstraint = latestSolution.additionalConstraints;
            Labels l = latestSolution.labels;
            if (l.getNamedVars().length == 0) {
                return solutions; // No named variables --> nothing to negate.
            }

            SubstitutedVar[] namedVars = l.getNamedVars();
            List<Constraint> disjunctionConstraints = new ArrayList<>();
            for (int i = 0; i < namedVars.length; i++) {
                SubstitutedVar sv = namedVars[i];
                if (sv instanceof Sprimitive) {
                    Constraint disjunctionConstraint = getNeq(sv, l.getLabelForNamedSubstitutedVar(sv));
                    disjunctionConstraints.add(disjunctionConstraint);
                }
            }

            Constraint newConstraint = Or.newInstance(disjunctionConstraints.toArray(new Constraint[0]));
            Constraint[] additionalSolutionConstraints = new Constraint[latestSolutionConstraint.length + 1];
            System.arraycopy(latestSolutionConstraint, 0 , additionalSolutionConstraints, 0, latestSolutionConstraint.length);
            additionalSolutionConstraints[latestSolutionConstraint.length] = newConstraint;
            solverManager.addConstraintAfterNewBacktrackingPoint(newConstraint);
            if (solverManager.isSatisfiable()) { // TODO unify with AbstractMulibExecutor
                Labels newLabels = LabelUtility.getLabels(
                        solverManager,
                        mulibValueTransformer.copyFromPrototype(),
                        l.getIdToNamedVar()
                );
                Object solutionValue = pathSolution.getLatestSolution().value;
                if (solutionValue instanceof Sym) {
                    solutionValue = l.getLabelForNamedSubstitutedVar((SubstitutedVar) solutionValue);
                }
                Solution newSolution = new Solution(
                        solutionValue,
                        newLabels,
                        additionalSolutionConstraints
                );
                pathSolution.addSolution(newSolution);
                solutions.add(newSolution);
            } else {
                break;
            }
        }
        return solutions;
    }

    private static Constraint getNeq(SubstitutedVar sv, Object value) {
        if (sv instanceof Conc) {
            return Sbool.FALSE;
        }
        if (sv instanceof Sbool) {
            Sbool bv = (Sbool) sv;
            Sbool bvv = Sbool.concSbool((boolean) value);
            return Xor.newInstance(bv, bvv);
        }
        if (sv instanceof Snumber) {
            Snumber wrappedPreviousValue;
            if (value instanceof Integer) {
                wrappedPreviousValue = Sint.concSint((Integer) value);
            } else if (value instanceof Double) {
                wrappedPreviousValue = Sdouble.concSdouble((Double) value);
            } else if (value instanceof Float) {
                wrappedPreviousValue = Sfloat.concSfloat((Float) value);
            } else if (value instanceof Long) {
                wrappedPreviousValue = Slong.concSlong((Long) value);
            } else if (value instanceof Short) {
                wrappedPreviousValue = Sshort.concSshort((Short) value);
            } else if (value instanceof Byte) {
                wrappedPreviousValue = Sbyte.concSbyte((Byte) value);
            } else {
                throw new NotYetImplementedException();
            }
            return Not.newInstance(Eq.newInstance((Snumber) sv, wrappedPreviousValue));
        } else {
            throw new NotYetImplementedException();
        }
    }
}
