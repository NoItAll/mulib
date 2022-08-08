package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.LabelingNotPossibleException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.solving.LabelUtility;
import de.wwu.mulib.solving.Labels;
import de.wwu.mulib.substitutions.*;
import de.wwu.mulib.substitutions.primitives.*;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

/**
 *
 * @param <M> Class representing a solver's model from which value assignments can be derived
 * @param <B> Class representing constraints in the solver
 * @param <AR> Class representing array expressions in the solver
 */
public abstract class AbstractIncrementalEnabledSolverManager<M, B, AR> implements SolverManager {
    private final IncrementalSolverState<AR> incrementalSolverState;
    private M currentModel;
    private boolean isSatisfiable;
    private boolean satisfiabilityWasCalculated;
    protected final boolean transformationRequired;
    protected final MulibConfig config;

    private final Map<Class<?>, BiFunction<SolverManager, Object, Object>> classesToLabelFunction;
    // Label cache
    private final Map<Object, Object> searchSpaceRepresentationToLabelObject = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    protected AbstractIncrementalEnabledSolverManager(MulibConfig config) {
        this.config = config;
        this.incrementalSolverState = IncrementalSolverState.newInstance(config);
        this.transformationRequired = config.TRANSF_TRANSFORMATION_REQUIRED;
        this.classesToLabelFunction = config.TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS;
    }

    @Override
    public void registerLabelPair(Object searchRegionRepresentation, Object labeled) {
        searchSpaceRepresentationToLabelObject.put(searchRegionRepresentation, labeled);
    }

    @Override
    public void setupForNewExecution() {
        searchSpaceRepresentationToLabelObject.clear();
    }

    @Override
    public final ArrayDeque<Constraint> getConstraints() {
        return new ArrayDeque<>(incrementalSolverState.getConstraints()); // Wrap and return
    }

    // For internal use without conservative copy
    protected ArrayDeque<Constraint> _getConstraints() {
        return incrementalSolverState.getConstraints();
    }

    @Override
    public final boolean checkWithNewConstraint(Constraint c) {
        if (c instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) c).isTrue();
        }
        B bool = transformConstraint(c);
        return _check(bool);
    }

    @Override
    public final boolean checkWithNewArraySelectConstraint(ArrayConstraint ac) {
        B bool = newArraySelectConstraint(incrementalSolverState.getCurrentArrayRepresentation(ac.getArrayId()), ac.getIndex(), ac.getValue());
        return _check(bool);
    }

    private boolean _check(B bool) {
        boolean result = calculateSatisfiabilityWithSolverBoolRepresentation(bool);
        _resetSatisfiabilityWasCalculatedAndModel();
        return result;
    }

    private void _resetSatisfiabilityWasCalculatedAndModel() {
        satisfiabilityWasCalculated = false;
        currentModel = null;
    }

    @Override
    public final List<ArrayConstraint> getArrayConstraints() {
        return incrementalSolverState.getArrayConstraints();
    }

    @Override
    public final boolean isSatisfiable() {
        assert incrementalSolverState.getLevel() != 0: "The initial choice should always be present";
        if (!satisfiabilityWasCalculated) {
            isSatisfiable = calculateIsSatisfiable();
            satisfiabilityWasCalculated = true;
        }
        return isSatisfiable;
    }

    @Override
    public final void addArrayConstraints(List<ArrayConstraint> acs) {
        for (ArrayConstraint ac : acs) {
            addArrayConstraint(ac);
        }
    }

    // Treatment of free arrays is inspired by that of Muli, yet modified. E.g., the ArrayConstraint is not a subtype of Constraint in Mulib:
    // https://github.com/wwu-pi/muggl/blob/53a2874cba2b193ec99d2aea8a454a88481656c7/muggl-solver-z3/src/main/java/de/wwu/muggl/solvers/z3/Z3MugglAdapter.java
    @Override
    public final void addArrayConstraint(ArrayConstraint ac) {
        incrementalSolverState.addArrayConstraint(ac);
        AR arrayRepresentation = incrementalSolverState.getCurrentArrayRepresentation(ac.getArrayId());
        if (ac.getType() == ArrayConstraint.Type.SELECT) {
            if (arrayRepresentation == null) {
                arrayRepresentation = createCompletelyNewArrayRepresentation(ac);
                incrementalSolverState.addRepresentationInitializingArrayConstraint(ac, arrayRepresentation);
            }
            addArraySelectConstraint(arrayRepresentation, ac.getIndex(), ac.getValue());
        } else {
            assert ac.getType() == ArrayConstraint.Type.STORE;
            if (arrayRepresentation == null) {
                arrayRepresentation = createCompletelyNewArrayRepresentation(ac);
            }
            arrayRepresentation = createNewArrayRepresentationForStore(ac, arrayRepresentation);
            incrementalSolverState.addRepresentationInitializingArrayConstraint(ac, arrayRepresentation);
        }
        _resetSatisfiabilityWasCalculatedAndModel();
    }

    @Override
    public final void addConstraint(Constraint c) {
        incrementalSolverState.addConstraint(c);
        _resetSatisfiabilityWasCalculatedAndModel();
        try {
            addSolverConstraintRepresentation(transformConstraint(c));
        } catch (Throwable t) {
            t.printStackTrace();
            throw new MulibRuntimeException(t);
        }
    }

    @Override
    public final void addConstraintAfterNewBacktrackingPoint(Constraint c) {
        _resetSatisfiabilityWasCalculatedAndModel();
        try {
            incrementalSolverState.pushConstraint(c);
            solverSpecificBacktrackingPoint();
            addSolverConstraintRepresentation(transformConstraint(c));
        } catch (Throwable t) {
            t.printStackTrace();
            throw new MulibRuntimeException(t);
        }
    }

    @Override
    public final void backtrackOnce() {
        solverSpecificBacktrackOnce();
        incrementalSolverState.popConstraint();
        _resetSatisfiabilityWasCalculatedAndModel();
    }

    @Override
    public final void backtrack(int numberOfChoiceOptions) {
        solverSpecificBacktrack(numberOfChoiceOptions);
        for (int i = 0; i < numberOfChoiceOptions; i++) {
            incrementalSolverState.popConstraint();
        }
        if (numberOfChoiceOptions > 0) {
            _resetSatisfiabilityWasCalculatedAndModel();
        }
    }

    @Override
    public final void backtrackAll() {
        backtrack(incrementalSolverState.getLevel());
    }

    @Override
    public final int getLevel() {
        return incrementalSolverState.getLevel();
    }

    @Override
    public List<Solution> getUpToNSolutions(final Solution initialSolution, AtomicInteger N) {
        Solution latestSolution = initialSolution;
        if (latestSolution.labels.getNamedVars().length == 0) {
            return Collections.singletonList(initialSolution); // No named variables --> nothing to negate.
        }
        List<Solution> solutions = new ArrayList<>();
        int backtrackAfter = 0;
        int currentN = N.get();
        while (currentN > 0) {
            Labels l = latestSolution.labels;

            SubstitutedVar[] namedVars = l.getNamedVars();
            List<Constraint> disjunctionConstraints = new ArrayList<>();
            for (SubstitutedVar sv : namedVars) {
                if (sv instanceof Sprimitive) {
                    Constraint disjunctionConstraint = getNeq(sv, l.getLabelForNamedSubstitutedVar(sv));
                    disjunctionConstraints.add(disjunctionConstraint);
                }
            }

            Constraint newConstraint = Or.newInstance(disjunctionConstraints);
            backtrackAfter++;
            addConstraintAfterNewBacktrackingPoint(newConstraint);
            if (isSatisfiable()) {
                Labels newLabels = LabelUtility.getLabels(
                        this,
                        l.getIdToNamedVar()
                );
                Object solutionValue = latestSolution.returnValue;
                if (solutionValue instanceof Sym) {
                    solutionValue = l.getLabelForNamedSubstitutedVar((SubstitutedVar) solutionValue);
                }
                Solution newSolution = new Solution(
                        solutionValue,
                        newLabels
                );
                currentN = N.decrementAndGet();
                solutions.add(newSolution);
                latestSolution = newSolution;
            } else {
                break;
            }
        }
        backtrack(backtrackAfter);
        solutions.add(initialSolution);
        return solutions;
    }

    @Override
    public Object getLabel(Object var) {
        if (!isSatisfiable()) {
            throw new LabelingNotPossibleException("Must be satisfiable.");
        } else if (var == null) {
            return null;
        }
        try {
            if (var instanceof Sprimitive) {
                return labelSprimitive((Sprimitive) var);
            }
            Object result;
            if ((result = searchSpaceRepresentationToLabelObject.get(var)) != null) {
                return result;
            }

            if (var instanceof Sarray) {
                result = labelSarray((Sarray<?>) var);
            } else if (var instanceof PartnerClass) {
                result = labelPartnerClassObject((PartnerClass) var);
            } else if (var.getClass().isArray()) {
                result = labelArray(var);
            } else {
                result = customLabelObject(var);
            }
            return result;
        } catch (Throwable t) {
            throw new MulibRuntimeException("Failed to get label", t);
        }
    }

    protected Object labelSprimitive(Sprimitive sprimitive) {
        if (sprimitive instanceof ConcSnumber) {
            return labelConcSnumber((ConcSnumber) sprimitive);
        }
        if (sprimitive instanceof Sbool.SymSbool) {
            Constraint c = ((Sbool.SymSbool) sprimitive).getRepresentedConstraint();
            if (c instanceof ConcolicConstraintContainer) {
                return ((ConcolicConstraintContainer) c).getConc().isTrue();
            }
        } else {
            NumericExpression ne = ((SymNumericExpressionSprimitive) sprimitive).getRepresentedExpression();
            if (ne instanceof ConcolicNumericContainer) {
                return labelConcSnumber(((ConcolicNumericContainer) ne).getConc());
            }
        }
        return labelSymSprimitive((SymSprimitive) sprimitive);
    }

    protected static Object labelConcSnumber(ConcSnumber searchRegionVal) {
        if (searchRegionVal instanceof Sbool) {
            return ((Sbool.ConcSbool) searchRegionVal).isTrue();
        }
        if (searchRegionVal instanceof Sint) {
            if (searchRegionVal instanceof Sshort) {
                return searchRegionVal.shortVal();
            } else if (searchRegionVal instanceof Sbyte) {
                return searchRegionVal.byteVal();
            } else {
                return searchRegionVal.intVal();
            }
        } else if (searchRegionVal instanceof Slong) {
            return searchRegionVal.longVal();
        } else if (searchRegionVal instanceof Sdouble) {
            return searchRegionVal.doubleVal();
        } else if (searchRegionVal instanceof Sfloat) {
            return searchRegionVal.floatVal();
        } else {
            throw new NotYetImplementedException();
        }
    }

    protected abstract Object labelSymSprimitive(SymSprimitive symSprimitive);

    protected Object labelSarray(Sarray<?> sarray) {
        int length = (Integer) labelSprimitive(sarray.getLength());
        Object[] result = new Object[length];
        searchSpaceRepresentationToLabelObject.put(sarray, result);
        Set<Sint> indices = sarray.getCachedIndices();
        for (Sint i : indices) {
            SubstitutedVar value = sarray.getForIndex(i);
            int labeledIndex = (Integer) labelSprimitive(i);
            Object labeledValue = getLabel(value);
            result[labeledIndex] = labeledValue;
        }
        return result;
    }

    protected Object labelPartnerClassObject(PartnerClass object) {
        if (transformationRequired) {
            Object emptyLabelObject = createEmptyLabelObject(object.getOriginalClass());
            searchSpaceRepresentationToLabelObject.put(object, emptyLabelObject);
            Object result = object.label(
                    emptyLabelObject,
                    this
            );
            assert emptyLabelObject == result;
            return result;
        } else {
            return object;
        }
    }

    protected Object labelArray(Object array) {
        int length = Array.getLength(array);
        Object[] result = new Object[length];
        searchSpaceRepresentationToLabelObject.put(array, result);
        for (int i = 0; i < length; i++) {
            result[i] = getLabel(Array.get(array, i));
        }
        return result;
    }

    protected Object customLabelObject(Object o) {
        BiFunction<SolverManager, Object, Object> labelMethod =
                this.classesToLabelFunction.get(o.getClass());
        if (labelMethod == null) {
            searchSpaceRepresentationToLabelObject.put(o, o);
            return o;
        } else {
            Object result = labelMethod.apply(this, o);
            searchSpaceRepresentationToLabelObject.put(o, result);
            return result;
        }
    }

    private Object createEmptyLabelObject(Class<?> clazz) {
        Constructor<?> constructor = getOrGenerateZeroArgsConstructor(clazz);
        try {
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new MulibRuntimeException(e);
        }
    }

    // Cache for generated constructors
    private static final Map<Class<?>, Constructor<?>> classToZeroArgsConstructor = Collections.synchronizedMap(new HashMap<>());
    protected static Constructor<?> getOrGenerateZeroArgsConstructor(final Class<?> toGenerateFor) {
        Class<?> current = toGenerateFor;
        // Gather superclasses
        ArrayDeque<Class<?>> deque = new ArrayDeque<>();
        Constructor<?> previousConstructor;
        do {
            previousConstructor = classToZeroArgsConstructor.get(current);
            if (previousConstructor != null) {
                break;
            }
            assert !deque.contains(current);
            deque.addFirst(current);
            current = current.getSuperclass();
        } while (current != null);

        ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
        Constructor<?> currentConstructor;
        // Starting from the most abstract class, generate suitable constructors
        while (!deque.isEmpty()) {
            current = deque.pollFirst();
            if (previousConstructor == null) {
                currentConstructor = rf.newConstructorForSerialization(current);
            } else {
                currentConstructor = rf.newConstructorForSerialization(current, previousConstructor);
            }
            if (currentConstructor == null) {
                throw new MulibRuntimeException("Failed to generate a constructor.");
            }
            classToZeroArgsConstructor.put(current, currentConstructor);
            previousConstructor = currentConstructor;
        }

        return classToZeroArgsConstructor.get(toGenerateFor);
    }

    protected static Constraint getNeq(SubstitutedVar sv, Object value) {
        if (sv instanceof Conc) {
            return Sbool.ConcSbool.FALSE;
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
                throw new NotYetImplementedException(sv.getClass().toString());
            }
            return Not.newInstance(Eq.newInstance((Snumber) sv, wrappedPreviousValue));
        } else {
            throw new NotYetImplementedException();
        }
    }

    protected final M getCurrentModel() {
        if (currentModel == null) {
            try {
                currentModel = calculateCurrentModel();
            } catch (Throwable t) {
                t.printStackTrace();
                throw new MulibRuntimeException(t);
            }
        }
        return currentModel;
    }

    protected abstract M calculateCurrentModel();

    protected abstract void addSolverConstraintRepresentation(B constraint);

    protected abstract boolean calculateIsSatisfiable();

    protected abstract AR createCompletelyNewArrayRepresentation(ArrayConstraint ac);

    protected abstract AR createNewArrayRepresentationForStore(ArrayConstraint ac, AR oldRepresentation);

    protected abstract void addArraySelectConstraint(AR arrayRepresentation, Sint index, SubstitutedVar value);

    protected abstract void solverSpecificBacktrackingPoint();

    protected abstract void solverSpecificBacktrackOnce();

    protected abstract void solverSpecificBacktrack(int toBacktrack);

    protected abstract boolean calculateSatisfiabilityWithSolverBoolRepresentation(B boolExpr);

    protected abstract B newArraySelectConstraint(AR arrayRepresentation, Sint indexInArray, SubstitutedVar arrayValue);

    protected abstract B transformConstraint(Constraint c);
}
