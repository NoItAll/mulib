package de.wwu.mulib;

import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.executors.MulibExecutorManager;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.solving.Labels;
import de.wwu.mulib.solving.Solvers;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.Conc;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformer.MulibTransformer;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class MulibContext {
    @SuppressWarnings("all")
    private final MulibConfig mulibConfig;
    @SuppressWarnings("all")
    private final MethodHandle methodHandle;
    private final MulibExecutorManager mulibExecutorManager;
    private final SolverManager solverManager;
    private final Supplier<List<Object>> argumentsSupplier;
    private final DeepCopyService deepCopyService;
    private final MulibTransformer mulibTransformer;

    protected MulibContext(
            MethodHandle methodHandle,
            MulibExecutorManager mulibExecutorManager,
            MulibConfig mulibConfig) {
        this.methodHandle = methodHandle;
        this.mulibExecutorManager = mulibExecutorManager;
        this.mulibConfig = mulibConfig;
        this.solverManager = Solvers.getSolverManager(mulibConfig);
        this.argumentsSupplier = null;
        this.deepCopyService = null;
        this.mulibTransformer = null;
    }

    protected MulibContext(
            MethodHandle methodHandle,
            MulibExecutorManager mulibExecutorManager,
            MulibConfig mulibConfig,
            List<Object> arguments, // With arguments
            MulibTransformer mulibTransformer) {
        this.methodHandle = methodHandle;
        this.mulibExecutorManager = mulibExecutorManager;
        this.mulibConfig = mulibConfig;
        this.solverManager = Solvers.getSolverManager(mulibConfig);
        this.deepCopyService = new DeepCopyService();
        this.argumentsSupplier = () -> {
            Object[] args = new Object[arguments.size()];
            for (int i = 0; i < arguments.size(); i++) {
                args[i] = deepCopyService.deepCopy(arguments.get(i));
            }
            return Arrays.asList(args);
        };
        this.mulibTransformer = mulibTransformer;
    }

    public synchronized List<PathSolution> getAllPathSolutionsWithArguments() {
        return mulibExecutorManager.getAllSolutions(argumentsSupplier.get());
    }

    public synchronized List<PathSolution> getAllPathSolutions() {
        return mulibExecutorManager.getAllSolutions();
    }

    public synchronized Optional<PathSolution> getPathSolution() {
        return mulibExecutorManager.getSolution();
    }

    public synchronized List<Solution> getAllSolutions(PathSolution pathSolution) {
        return getUpToNSolutions(pathSolution, Integer.MAX_VALUE);
    }


    public synchronized List<Solution> getUpToNSolutions(PathSolution pathSolution, int N) {
        if (pathSolution.getCurrentlyInitializedSolutions().size() >= N) {
            return new ArrayList<>(pathSolution.getCurrentlyInitializedSolutions());
        }
        solverManager.backtrackAll();
        List<Constraint> constraintList = new ArrayList<>();
        constraintList.add(Sbool.TRUE);
        constraintList.addAll(Arrays.asList(pathSolution.getPathConstraints()));
        solverManager.addConstraintsAfterNewBacktrackingPoint(constraintList);
        List<Solution> solutions = new ArrayList<>(pathSolution.getCurrentlyInitializedSolutions());
        while (solverManager.isSatisfiable() && solutions.size() < N) {
            Solution latestSolution = pathSolution.getLatestSolution();
            Constraint[] latestSolutionConstraint = latestSolution.additionalConstraints;
            Labels l = latestSolution.labels;
            if (l.getTrackedVariables().length == 0) {
                return solutions; // No tracked variables --> nothing to negate.
            }

            SubstitutedVar[] trackedVars = l.getTrackedVariables();
            Constraint[] disjunctionConstraints = new Constraint[trackedVars.length];
            for (int i = 0; i < trackedVars.length; i++) {
                SubstitutedVar sv = trackedVars[i];
                Constraint disjunctionConstraint = getNeq(sv, l.getForTrackedSubstitutedVar(sv));
                disjunctionConstraints[i] = disjunctionConstraint;
            }

            Constraint newConstraint = Or.newInstance(disjunctionConstraints);
            Constraint[] additionalSolutionConstraints = new Constraint[latestSolutionConstraint.length + 1];
            System.arraycopy(latestSolutionConstraint, 0 , additionalSolutionConstraints, 0, latestSolutionConstraint.length);
            additionalSolutionConstraints[latestSolutionConstraint.length] = newConstraint;
            solverManager.addConstraintAfterNewBacktrackingPoint(newConstraint);
            if (solverManager.isSatisfiable()) {
                Labels newLabels = solverManager.getLabels(l.getIdentifiersToSVars());
                Object solutionValue = pathSolution.getLatestSolution().value;
                if (solutionValue instanceof Sym) {
                    solutionValue = l.getForTrackedSubstitutedVar((SubstitutedVar) solutionValue);
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
        if (sv instanceof Snumber) {
            Snumber wrappedPreviousValue;
            if (value instanceof Integer) {
                wrappedPreviousValue = Sint.newConcSint((Integer) value);
            } else if (value instanceof Double) {
                wrappedPreviousValue = Sdouble.newConcSdouble((Double) value);
            } else if (value instanceof Float) {
                wrappedPreviousValue = Sfloat.newConcSfloat((Float) value);
            } else {
                throw new NotYetImplementedException();
            }
            return Not.newInstance(Eq.newInstance((Snumber) sv, wrappedPreviousValue));
        } else if (sv instanceof Sbool) {
            Sbool bv = (Sbool) sv;
            Sbool.ConcSbool bvv = Sbool.newConcSbool((boolean) value);
            return Xor.newInstance(bv, bvv);
        } else {
            throw new NotYetImplementedException();
        }
    }
}
