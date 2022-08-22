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
import de.wwu.mulib.solving.object_representations.ArraySolverRepresentation;
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
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractIncrementalEnabledSolverManager<M, B, AR> implements SolverManager {

    // Raw use in this abstract superclass so that sub-classes can overwrite with their specific array representations
    // while we can still use a own layer or high-level array theory
    private final IncrementalSolverState incrementalSolverState;
    private M currentModel;
    private boolean isSatisfiable;
    private boolean satisfiabilityWasCalculated;
    protected final boolean transformationRequired;
    protected final MulibConfig config;

    private final Map<Class<?>, BiFunction<SolverManager, Object, Object>> classesToLabelFunction;
    // Label cache
    private final Map<Object, Object> searchSpaceRepresentationToLabelObject = new IdentityHashMap<>();

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
    public final boolean checkWithNewArrayConstraint(ArrayConstraint ac) {
        if (ac.getType() == ArrayConstraint.Type.STORE) {
            // It is always possible to store a new value in an array
            return true;
        }
        // If we select, we must check if the selected value can legally be contained in the array
        boolean result;
        if (config.HIGH_LEVEL_FREE_ARRAY_THEORY) {
            result = _checkWithNewFreeArrayCompatibilityLayerArraySelectConstraint(ac);
        } else {
            // Solver specific treatment
            result = _checkWithNewSolverSpecificArraySelectConstraint(ac);
        }
        return result;
    }

    private boolean _checkWithNewFreeArrayCompatibilityLayerArraySelectConstraint(ArrayConstraint ac) {
        ArraySolverRepresentation asr = (ArraySolverRepresentation) incrementalSolverState.getCurrentArrayRepresentation(ac.getArrayId());
        // Copy is needed as otherwise ArraySolverRepresentation is mutated by means of select
        ArraySolverRepresentation copy = new ArraySolverRepresentation(asr, asr.getLevel());
        Constraint selectConstraint = copy.select(ac.getIndex(), ac.getValue());
        boolean result = checkWithNewConstraint(selectConstraint);
        _resetSatisfiabilityWasCalculatedAndModel();
        return result;
    }

    private boolean _checkWithNewSolverSpecificArraySelectConstraint(ArrayConstraint ac) {
        B bool = newArraySelectConstraint((AR) incrementalSolverState.getCurrentArrayRepresentation(ac.getArrayId()), ac.getIndex(), ac.getValue());
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
    // It was also extended by an own alternative abstraction layer
    @Override
    public final void addArrayConstraint(ArrayConstraint ac) {
        if (config.HIGH_LEVEL_FREE_ARRAY_THEORY) {
            _freeArrayCompatibilityLayerArrayConstraintTreatement(ac);
        } else {
            // Solver specific treatment
            _solverSpecificArrayConstraintTreatment(ac);
        }
        incrementalSolverState.addArrayConstraint(ac);
        _resetSatisfiabilityWasCalculatedAndModel();
    }

    private void _freeArrayCompatibilityLayerArrayConstraintTreatement(ArrayConstraint ac) {
        ArraySolverRepresentation arrayRepresentation =
                (ArraySolverRepresentation) incrementalSolverState.getCurrentArrayRepresentation(ac.getArrayId());
        if (ac.getType() == ArrayConstraint.Type.SELECT) {
            if (arrayRepresentation == null) {
                arrayRepresentation = new ArraySolverRepresentation(ac.getArrayId(), getLevel());
                incrementalSolverState.addRepresentationInitializingArrayConstraint(ac, arrayRepresentation);
            } else if (arrayRepresentation.getLevel() != getLevel()) {
                arrayRepresentation = new ArraySolverRepresentation(arrayRepresentation, getLevel());
                incrementalSolverState.addRepresentationInitializingArrayConstraint(ac, arrayRepresentation);
            }
            Constraint arraySelectConstraint = arrayRepresentation.select(ac.getIndex(), ac.getValue());
            addConstraint(arraySelectConstraint);
        } else {
            // Is store
            assert ac.getType() == ArrayConstraint.Type.STORE;
            if (arrayRepresentation == null) {
                arrayRepresentation = new ArraySolverRepresentation(ac.getArrayId(), getLevel());
            } else if (arrayRepresentation.getLevel() != getLevel()) {
                arrayRepresentation = new ArraySolverRepresentation(arrayRepresentation, getLevel());
                incrementalSolverState.addRepresentationInitializingArrayConstraint(ac, arrayRepresentation);
            }
            arrayRepresentation = arrayRepresentation.store(ac.getIndex(), ac.getValue(), getLevel());
            incrementalSolverState.addRepresentationInitializingArrayConstraint(ac, arrayRepresentation);
        }
    }

    private void _solverSpecificArrayConstraintTreatment(ArrayConstraint ac) {
        AR arrayRepresentation = (AR) incrementalSolverState.getCurrentArrayRepresentation(ac.getArrayId());
        if (ac.getType() == ArrayConstraint.Type.SELECT) {
            if (arrayRepresentation == null) {
                arrayRepresentation = createCompletelyNewArrayRepresentation(ac);
                incrementalSolverState.addRepresentationInitializingArrayConstraint(ac, arrayRepresentation);
            }
            addArraySelectConstraint(arrayRepresentation, ac.getIndex(), ac.getValue());
            _resetSatisfiabilityWasCalculatedAndModel();
        } else {
            assert ac.getType() == ArrayConstraint.Type.STORE;
            if (arrayRepresentation == null) {
                arrayRepresentation = createCompletelyNewArrayRepresentation(ac);
            }
            arrayRepresentation = createNewArrayRepresentationForStore(ac, arrayRepresentation);
            incrementalSolverState.addRepresentationInitializingArrayConstraint(ac, arrayRepresentation);
        }
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
        solutions.add(initialSolution);
        // Decrement to account for initialSolution
        int currentN = N.decrementAndGet();
        int backtrackAfterwards = 0;
        while (currentN > 0) {
            Labels l = latestSolution.labels;

            List<Constraint> disjunctionConstraints = getNeqConstraints(l);
            if (disjunctionConstraints.isEmpty()) {
                // Nothing to negate
                break;
            }

            Constraint newConstraint = Or.newInstance(disjunctionConstraints);
            backtrackAfterwards++;
            addConstraintAfterNewBacktrackingPoint(newConstraint);
            if (isSatisfiable()) {
                Labels newLabels = LabelUtility.getLabels(
                        this,
                        l.getIdToNamedVar()
                );
                Object solutionValue = newLabels.getIdToLabel().get("return");
                if (solutionValue instanceof Sym) {
                    solutionValue = newLabels.getLabelForNamedSubstitutedVar((SubstitutedVar) solutionValue);
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
        backtrack(backtrackAfterwards);
        return solutions;
    }

    private List<Constraint> getNeqConstraints(Labels givenLabels) {
        SubstitutedVar[] namedVars = givenLabels.getNamedVars();
        List<Constraint> disjunctionConstraints = new ArrayList<>();
        for (SubstitutedVar sv : namedVars) {
            if (sv instanceof Conc) {
                // It does not help to negate concrete values
                continue;
            }
            Constraint disjunctionConstraint;
            if (sv instanceof Sprimitive || sv instanceof Sarray) {
                Object label = givenLabels.getLabelForNamedSubstitutedVar(sv);
                disjunctionConstraint = getNeq(sv, label);
                disjunctionConstraints.add(disjunctionConstraint);
            } else {
                throw new NotYetImplementedException(); // TODO implement, also in getNeq
            }
        }
        return disjunctionConstraints;
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
                sprimitive = ((ConcolicConstraintContainer) c).getSym();
            }
        } else {
            NumericExpression ne = ((SymNumericExpressionSprimitive) sprimitive).getRepresentedExpression();
            if (ne instanceof ConcolicNumericContainer) {
                sprimitive = ((ConcolicNumericContainer) ne).getSym();
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
        if (sarray.onlyConcreteIndicesUsed()) {
            // In this case the constraints did not need to be manifested and we can use the cache
            for (Sint index : sarray.getCachedIndices()) {
                Integer labeledIndex = (Integer) labelSprimitive(index);
                SubstitutedVar cachedValue = sarray.getFromCacheForIndex(index);
                Object labeledValue = getLabel(cachedValue);
                result[labeledIndex] = labeledValue;
            }
        } else {
            // In this case, the constraints were propagated to the constraint solver and accurately describe the
            // state changes of the array
            ArrayConstraint[] arrayConstraints = getArrayConstraintsForSarray(sarray);
            for (ArrayConstraint ac : arrayConstraints) {
                Integer labeledIndex = (Integer) labelSprimitive(ac.getIndex());
                Object labeledValue = getLabel(ac.getValue());
                result[labeledIndex] = labeledValue;
            }
        }
        return result;
    }

    private ArrayConstraint[] getArrayConstraintsForSarray(Sarray sarray) {
        return getArrayConstraints().stream()
                .filter(ac -> ac.getArrayId().equals(sarray.getId()))
                .toArray(ArrayConstraint[]::new);
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

    protected Constraint getNeq(SubstitutedVar sv, Object value) {
        if (sv instanceof Conc) {
            return Sbool.ConcSbool.FALSE;
        }
        if (sv instanceof Sbool) {
            Sbool bv = ConcolicConstraintContainer.tryGetSymFromConcolic((Sbool) sv);
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
            return Not.newInstance(Eq.newInstance(ConcolicNumericContainer.tryGetSymFromConcolic((Snumber) sv), wrappedPreviousValue));
        } if (sv instanceof Sarray) {
            Constraint result = Sbool.ConcSbool.FALSE;
            Sarray sarray = (Sarray) sv;
            Constraint disjunctionConstraint;
            if (sarray.onlyConcreteIndicesUsed()) {
                Set<Sint> indices = sarray.getCachedIndices();
                for (Sint index : indices) {
                    SubstitutedVar cachedValue = sarray.getFromCacheForIndex(index);
                    Object label = getLabel(cachedValue);
                    disjunctionConstraint = getNeq(cachedValue, label);
                    result = Or.newInstance(result, disjunctionConstraint);
                }
            } else {
                ArrayConstraint[] acs = getArrayConstraintsForSarray((Sarray) sv);
                for (ArrayConstraint ac : acs) {
                    SubstitutedVar val = ac.getValue();
                    Object label = getLabel(val);
                    Sint index = ac.getIndex();
                    Object indexLabel = getLabel(index);
                    disjunctionConstraint = getNeq(val, label);
                    result = Or.newInstance(result, disjunctionConstraint, getNeq(index, indexLabel));
                }
            }
            return result;
        } else if (sv instanceof PartnerClass) {
            return Sbool.ConcSbool.FALSE; // TODO Not yet capable of doing this
        } else {
            throw new NotYetImplementedException(sv.getClass().toString());
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
