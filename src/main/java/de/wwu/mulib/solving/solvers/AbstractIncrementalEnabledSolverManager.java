package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.LabelingNotPossibleException;
import de.wwu.mulib.exceptions.MisconfigurationException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.solving.ArrayInformation;
import de.wwu.mulib.solving.LabelUtility;
import de.wwu.mulib.solving.Labels;
import de.wwu.mulib.solving.PartnerClassObjectInformation;
import de.wwu.mulib.solving.object_representations.AliasingArraySolverRepresentation;
import de.wwu.mulib.solving.object_representations.AliasingPartnerClassObjectSolverRepresentation;
import de.wwu.mulib.solving.object_representations.ArraySolverRepresentation;
import de.wwu.mulib.solving.object_representations.PartnerClassObjectSolverRepresentation;
import de.wwu.mulib.substitutions.*;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.StringConstants;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

/**
 *
 * @param <M> Class representing a solver's model from which value assignments can be derived
 * @param <B> Class representing constraints in the solver
 * @param <AR> Class representing array expressions in the solver
 * @param <PR> Class representing non-array partner class objects in the solver
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractIncrementalEnabledSolverManager<M, B, AR, PR> implements SolverManager {

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
    private final Map<Object, Object> _searchSpaceRepresentationToLabelObject = new IdentityHashMap<>();

    protected AbstractIncrementalEnabledSolverManager(MulibConfig config) {
        this.config = config;
        this.incrementalSolverState = IncrementalSolverState.newInstance(config);
        this.transformationRequired = config.TRANSF_TRANSFORMATION_REQUIRED;
        this.classesToLabelFunction = config.TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS;
    }

    @Override
    public void registerLabelPair(Object searchRegionRepresentation, Object labeled) {
        if (searchRegionRepresentation instanceof PartnerClass && ((PartnerClass) searchRegionRepresentation).__mulib__getId() != null) {
            searchRegionRepresentation = ((PartnerClass) searchRegionRepresentation).__mulib__getId();
        }
        assert !_searchSpaceRepresentationToLabelObject.containsKey(searchRegionRepresentation);
        _searchSpaceRepresentationToLabelObject.put(searchRegionRepresentation, labeled);
    }

    @Override
    public void resetLabels() {
        _searchSpaceRepresentationToLabelObject.clear();
    }

    private Object checkForAlreadyLabeledRepresentation(Object toLabel) {
        if (toLabel instanceof PartnerClass && ((PartnerClass) toLabel).__mulib__getId() != null) {
           toLabel =  ((PartnerClass) toLabel).__mulib__getId();
        }
        Object result = _searchSpaceRepresentationToLabelObject.get(toLabel);
        if (result == null && toLabel instanceof Sint.SymSint) {
            result = _searchSpaceRepresentationToLabelObject.get(Sint.concSint(_labelSintToInt((Sint) toLabel)));
        }
        return result;
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
    public PartnerClassObjectInformation getAvailableInformationOnPartnerClassObject(Sint id, String field) {
        if (!config.HIGH_LEVEL_FREE_ARRAY_THEORY) {
            // TODO Potentially implement for solver-internal array theories
            throw new MisconfigurationException("The config option HIGH_LEVEL_FREE_ARRAY_THEORY must be set");
        }
        IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps =
                incrementalSolverState.getSymbolicPartnerClassObjectStates();
        return new PartnerClassObjectInformation(sps, sps.getRepresentationForId(id).getNewestRepresentation(), field);
    }

    @Override
    public ArrayInformation getAvailableInformationOnArray(Sint id) {
        if (!config.HIGH_LEVEL_FREE_ARRAY_THEORY) {
            // TODO Potentially implement for solver-internal array theories
            throw new MisconfigurationException("The config option HIGH_LEVEL_FREE_ARRAY_THEORY must be set");
        }
        IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> sas =
                incrementalSolverState.getSymbolicArrayStates();
        return new ArrayInformation(sas, sas.getRepresentationForId(id).getNewestRepresentation());
    }

    @Override
    public final List<PartnerClassObjectConstraint> getPartnerClassObjectConstraints() {
        List<PartnerClassObjectConstraint> result = _getNonArrayPartnerClassObjectConstraints();
        result.addAll(_getArrayConstraints());
        return result;
    }

    private List<ArrayConstraint> _getArrayConstraints() {
        return incrementalSolverState.getArrayConstraints();
    }

    private List<PartnerClassObjectConstraint> _getNonArrayPartnerClassObjectConstraints() {
        return incrementalSolverState.getNonArrayPartnerClassObjectConstraints();
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
    public final void addPartnerClassObjectConstraints(List<PartnerClassObjectConstraint> acs) {
        for (PartnerClassObjectConstraint ac : acs) {
            this.addPartnerClassObjectConstraint(ac);
        }
    }

    // Treatment of free arrays is inspired by that of Muli, yet modified. E.g., the ArrayConstraint is not a subtype of Constraint in Mulib:
    // https://github.com/wwu-pi/muggl/blob/53a2874cba2b193ec99d2aea8a454a88481656c7/muggl-solver-z3/src/main/java/de/wwu/muggl/solvers/z3/Z3MugglAdapter.java
    // It was also extended by an own alternative abstraction layer
    @Override
    public final void addPartnerClassObjectConstraint(PartnerClassObjectConstraint ic) {
        if (ic instanceof ArrayConstraint) {
            addArrayConstraint((ArrayConstraint) ic);
        } else {
            addNonArrayPartnerClassObjectConstraint(ic);
        }
    }

    private void addNonArrayPartnerClassObjectConstraint(PartnerClassObjectConstraint pc) {
        assert !(pc instanceof ArrayConstraint);
        if (config.HIGH_LEVEL_FREE_ARRAY_THEORY) {
            if (pc instanceof PartnerClassObjectFieldConstraint) {
                _objectCompatibilityLayerFieldAccessTreatment((PartnerClassObjectFieldConstraint) pc);
                incrementalSolverState.addPartnerClassObjectConstraint(pc);
            } else if (pc instanceof PartnerClassObjectInitializationConstraint) {
                PartnerClassObjectInitializationConstraint pic = (PartnerClassObjectInitializationConstraint) pc;
                PartnerClassObjectSolverRepresentation partnerClassObjectSolverRepresentation =
                        PartnerClassObjectSolverRepresentation.newInstance(
                                config,
                                pic,
                                incrementalSolverState.getSymbolicArrayStates(),
                                incrementalSolverState.getSymbolicPartnerClassObjectStates(),
                                getLevel()
                        );
                incrementalSolverState.initializePartnerClassObjectRepresentation(
                        pic,
                        partnerClassObjectSolverRepresentation
                );
                incrementalSolverState.addPartnerClassObjectConstraint(pic);
                if (partnerClassObjectSolverRepresentation instanceof AliasingPartnerClassObjectSolverRepresentation) {
                    addConstraint(((AliasingPartnerClassObjectSolverRepresentation) partnerClassObjectSolverRepresentation).getMetadataConstraintForPotentialIds());
                }
            } else {
                throw new NotYetImplementedException();
            }
        } else {
            // TODO
            throw new NotYetImplementedException("Currently, only the implementation of symbolic aliasing of objects with " +
                    "the high-level array theory has been validated");
        }
    }

    private void _objectCompatibilityLayerFieldAccessTreatment(PartnerClassObjectFieldConstraint pc) {
        PartnerClassObjectSolverRepresentation rep =
                (PartnerClassObjectSolverRepresentation)
                        incrementalSolverState.getCurrentPartnerClassObjectRepresentation(pc.getPartnerClassObjectId());
        assert rep != null;
        if (rep.getLevel() != getLevel()) {
            rep = rep.copyForNewLevel(getLevel());
            incrementalSolverState.addNewRepresentationInitializingPartnerClassFieldConstraint(pc, rep);
        }
        if (pc.getType() == PartnerClassObjectFieldConstraint.Type.GETFIELD) {
            Constraint getFieldConstraint = rep.getField(pc.getFieldName(), pc.getValue());
            addConstraint(getFieldConstraint);
        } else {
            assert pc.getType() == PartnerClassObjectFieldConstraint.Type.PUTFIELD;
            rep.putField(pc.getFieldName(), pc.getValue());
        }
    }

    private void addArrayConstraint(ArrayConstraint ac) {
        if (config.HIGH_LEVEL_FREE_ARRAY_THEORY) {
            if (ac instanceof ArrayAccessConstraint) {
                _freeArrayCompatibilityLayerArrayConstraintTreatement((ArrayAccessConstraint) ac);
                incrementalSolverState.addArrayConstraint(ac);
            } else {
                assert ac instanceof ArrayInitializationConstraint;
                ArrayInitializationConstraint aic = (ArrayInitializationConstraint) ac;
                // The addition of the initial elements is taken cae of in the instance of ArraySolverRepresentation
                ArraySolverRepresentation arraySolverRepresentation =
                        ArraySolverRepresentation.newInstance(
                                config,
                                aic,
                                incrementalSolverState.getSymbolicArrayStates(),
                                incrementalSolverState.getSymbolicPartnerClassObjectStates(),
                                getLevel()
                        );
                incrementalSolverState.initializeArrayRepresentation(
                        aic,
                        arraySolverRepresentation
                );
                incrementalSolverState.addArrayConstraint(aic);
                if (arraySolverRepresentation instanceof AliasingArraySolverRepresentation) {
                    // Restrict length, isNull, and id of aliasing array
                    addConstraint(((AliasingArraySolverRepresentation) arraySolverRepresentation).getMetadataConstraintForPotentialIds());
                }
            }
        } else {
            if (ac instanceof ArrayAccessConstraint) {
                // Solver specific treatment
                _solverSpecificArrayConstraintTreatment((ArrayAccessConstraint) ac);
                incrementalSolverState.addArrayConstraint(ac);
            } else {
                assert ac instanceof ArrayInitializationConstraint;
                ArrayInitializationConstraint aic = (ArrayInitializationConstraint) ac;
                incrementalSolverState.initializeArrayRepresentation(
                        aic,
                        createCompletelyNewArrayRepresentation(aic)
                );
                incrementalSolverState.addArrayConstraint(aic);
                // Initialize the initial content of the sarray
                for (ArrayAccessConstraint aac : aic.getInitialSelectConstraints()) {
                    _solverSpecificArrayConstraintTreatment(aac);
                }
            }
        }
    }

    private void _freeArrayCompatibilityLayerArrayConstraintTreatement(ArrayAccessConstraint ac) {
        ArraySolverRepresentation arrayRepresentation =
                (ArraySolverRepresentation) incrementalSolverState.getCurrentArrayRepresentation(ac.getPartnerClassObjectId());
        assert arrayRepresentation != null;
        if (arrayRepresentation.getLevel() != getLevel()) {
            arrayRepresentation = arrayRepresentation.copyForNewLevel(getLevel());
            incrementalSolverState.addNewRepresentationInitializingArrayConstraint(ac, arrayRepresentation);
        }
        if (ac.getType() == ArrayAccessConstraint.Type.SELECT) {
            Constraint arraySelectConstraint = arrayRepresentation.select(ac.getIndex(), ac.getValue());
            // Constraint is reset in addConstraint(...)
            addConstraint(arraySelectConstraint);
        } else {
            // Is store
            assert ac.getType() == ArrayAccessConstraint.Type.STORE;
            arrayRepresentation.store(ac.getIndex(), ac.getValue());
        }
    }

    private void _solverSpecificArrayConstraintTreatment(ArrayAccessConstraint ac) {
        AR arrayRepresentation = (AR) incrementalSolverState.getCurrentArrayRepresentation(ac.getPartnerClassObjectId());
        assert arrayRepresentation != null;
        if (ac.getType() == ArrayAccessConstraint.Type.SELECT) {
            addArraySelectConstraint(arrayRepresentation, ac.getIndex(), ac.getValue());
            _resetSatisfiabilityWasCalculatedAndModel();
        } else {
            assert ac.getType() == ArrayAccessConstraint.Type.STORE;
            arrayRepresentation = createNewArrayRepresentationForStore(ac, arrayRepresentation);
            incrementalSolverState.addNewRepresentationInitializingArrayConstraint(ac, arrayRepresentation);
        }
    }

    @Override
    public final void addConstraint(Constraint c) {
        if (c instanceof Sbool.ConcSbool && ((Sbool.ConcSbool) c).isTrue()) {
            return;
        }
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
        // Decrement to account for initialSolution
        int currentN = N.decrementAndGet();
        if (latestSolution.labels.getNamedVars().length == 0) {
            return Collections.singletonList(initialSolution); // No named variables --> nothing to negate.
        }
        List<Solution> solutions = new ArrayList<>();
        solutions.add(initialSolution);
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
                resetLabels();
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
            if (sv instanceof Sprimitive || sv instanceof PartnerClass) {
                Object label = givenLabels.getLabelForNamedSubstitutedVar(sv);
                disjunctionConstraint = getNeq(sv, label);
                disjunctionConstraints.add(disjunctionConstraint);
            } else {
                throw new NotYetImplementedException();
            }
        }
        return disjunctionConstraints;
    }

    @Override
    public final void shutdown() {
        _searchSpaceRepresentationToLabelObject.clear();
        incrementalSolverState.clear();
        solverSpecificShutdown();
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
            if ((result = checkForAlreadyLabeledRepresentation(var)) != null) {
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
        Object result;
        if (!sarray.__mulib__shouldBeRepresentedInSolver()) {
            int length = _labelSintToInt(sarray._getLengthWithoutCheckingForIsNull());
            Object array = Array.newInstance(transformNonSarrayMulibTypeToJavaType(sarray.getElementType()), length);
            registerLabelPair(sarray, array);
            // In this case the constraints did not need to be manifested and we can use the cache
            for (Sint index : sarray.getCachedIndices()) {
                int labeledIndex = _labelSintToInt(index);
                SubstitutedVar cachedValue = sarray.getFromCacheForIndex(index);
                Object labeledValue = getLabel(cachedValue);
                Array.set(array, labeledIndex, labeledValue);
            }
            result = array;
        } else {
            // In this case, the constraints were propagated to the constraint solver and accurately describe the
            // state changes of the array
            assert sarray.__mulib__getId() != null;
            result = labelRepresentedArray(sarray.__mulib__getId());
        }
        return result;
    }

    private Object labelRepresentedArray(Sint arrayId) {
        assert arrayId != null;
        if (_labelSintToInt(arrayId) == -1) {
            return null;
        }
        Object array;
        if ((array = checkForAlreadyLabeledRepresentation(arrayId)) != null) {
            return array;
        }
        ArrayConstraint[] constraints = getArrayConstraintsForSarray(arrayId);
        assert constraints.length > 0;
        assert constraints[0] instanceof ArrayInitializationConstraint;
        ArrayInitializationConstraint aic = (ArrayInitializationConstraint) constraints[0];
        // Determine type of array
        Class<?> type = aic.getValueType();
        Class<?> originalType = transformNonSarrayMulibTypeToJavaType(type);
        int length = _labelSintToInt(aic.getArrayLength());
        // Create array of suiting type
        array = Array.newInstance(originalType, length);
        boolean isNestedArray = type.isArray();
        registerLabelPair(arrayId, array);
        if (arrayId instanceof Sint.SymSint) {
            registerLabelPair(Sint.concSint(_labelSintToInt(arrayId)), array);
        }
        ArrayAccessConstraint[] initialSelects = aic.getInitialSelectConstraints();
        for (ArrayAccessConstraint s : initialSelects) {
            setInArray(array, s, type, isNestedArray);
        }
        for (int i = 1; i < constraints.length; i++) {
            if (constraints[i] instanceof ArrayInitializationConstraint) {
                continue; // Can happen during aliasing; - we just take the first initialization constraint in this case
            }
            assert constraints[i] instanceof ArrayAccessConstraint;
            ArrayAccessConstraint s = (ArrayAccessConstraint) constraints[i];
            setInArray(array, s, type, isNestedArray);
        }
        return array;
    }

    private void setInArray(Object array, ArrayAccessConstraint s, Class<?> type, boolean isNestedArray) {
        int index = _labelSintToInt(s.getIndex());
        Object val;
        if (isNestedArray) {
            // Array values are arrays themselves
            val = labelRepresentedArray((Sint) s.getValue());
        } else {
            if (Sprimitive.class.isAssignableFrom(type)) {
                val = labelSprimitive(s.getValue());
            } else {
                assert PartnerClass.class.isAssignableFrom(type);
                val = labelPartnerClassObject((Sint) s.getValue());
            }
        }
        Array.set(array, index, val);
    }

    private Object labelPartnerClassObject(Sint partnerClassObjectId) {
        assert partnerClassObjectId != null;
        if (_labelSintToInt(partnerClassObjectId) == -1) {
            return null;
        }
        Object object;
        if ((object = checkForAlreadyLabeledRepresentation(partnerClassObjectId)) != null) {
            return object;
        }
        PartnerClassObjectConstraint[] constraints = getConstraintsForPartnerClassObject(partnerClassObjectId);
        assert constraints.length > 0;
        assert constraints[0] instanceof PartnerClassObjectInitializationConstraint;
        PartnerClassObjectInitializationConstraint pic = (PartnerClassObjectInitializationConstraint) constraints[0];
        Class<?> originalType = transformNonArrayPartnerClassTypeToJavaType(pic.getClazz());
        object = createEmptyLabelObject(originalType);
        registerLabelPair(partnerClassObjectId, object);
        if (partnerClassObjectId instanceof Sint.SymSint) {
            registerLabelPair(Sint.concSint(_labelSintToInt(partnerClassObjectId)), object);
        }
        Field[] fields = originalType.getDeclaredFields(); // TODO Merge into label-method
        Arrays.stream(fields).forEach(f -> f.setAccessible(true));
        for (PartnerClassObjectFieldConstraint g : pic.getInitialGetfields()) {
            setFieldInLabelObject(object, fields, g);
        }
        for (int i = 1; i < constraints.length; i++) {
            if (constraints[i] instanceof PartnerClassObjectInitializationConstraint) {
                continue; // Can happen during aliasing; - we just take the first initialization constraint in this case
            }
            assert constraints[i] instanceof PartnerClassObjectFieldConstraint;
            PartnerClassObjectFieldConstraint g = (PartnerClassObjectFieldConstraint) constraints[i];
            setFieldInLabelObject(object, fields, g);
        }
        return object;
    }


    private Class<?> transformNonSarrayMulibTypeToJavaType(Class<?> c) {
        assert !Sarray.class.isAssignableFrom(c);
        if (c.isArray()) {
            Class<?> innermostType = c.getComponentType();
            int numberDims = 1;
            while (innermostType.isArray()) {
                innermostType = innermostType.getComponentType();
                numberDims++;
            }
            Class<?> innermostOriginalType = transformNonSarrayMulibTypeToJavaType(innermostType);
            Class<?> result = innermostOriginalType;
            while (numberDims != 0) {
                result = Array.newInstance(result, 0).getClass();
                numberDims--;
            }
            return result;
        } else if (Sint.class.isAssignableFrom(c)) {
            if (Sbool.class.isAssignableFrom(c)) {
                return boolean.class;
            } else if (Sshort.class.isAssignableFrom(c)) {
                return short.class;
            } else if (Sbyte.class.isAssignableFrom(c)) {
                return byte.class;
            } else {
                assert Sint.class == c;
                return int.class;
            }
        } else if (Sdouble.class.isAssignableFrom(c)) {
            return double.class;
        } else if (Slong.class.isAssignableFrom(c)) {
            return long.class;
        } else if (Sfloat.class.isAssignableFrom(c)) {
            return float.class;
        } else {
            return transformNonArrayPartnerClassTypeToJavaType(c);
        }
    }

    private void setFieldInLabelObject(Object object, Field[] fields, PartnerClassObjectFieldConstraint g) {
        Field f = null;
        for (Field fo : fields) {
            if (fo.getName().equals(g.getFieldName())) {
                f = fo;
                break;
            }
        }
        if (f == null) {
            throw new LabelingNotPossibleException("Field for field constraint not found: " + g.getFieldName());
        }
        Object val;
        if (f.getType().isArray()) {
            assert g.getValue() instanceof Sint;
            val = labelRepresentedArray((Sint) g.getValue());
        } else if (f.getType().isPrimitive()) {
            val = labelSprimitive(g.getValue());
        } else {
            assert g.getValue() instanceof Sint;
            val = labelPartnerClassObject((Sint) g.getValue());
        }
        try {
            f.set(object, val);
        } catch (Exception e) {
            e.printStackTrace();
            throw new LabelingNotPossibleException("Setting value failed");
        }
    }

    private Class<?> transformNonArrayPartnerClassTypeToJavaType(Class<?> c) {
        assert !Sarray.class.isAssignableFrom(c) && (PartnerClass.class.isAssignableFrom(c) || !c.getName().contains(StringConstants._TRANSFORMATION_PREFIX));
        String className = c.getName();
        try {
            Class<?> originalClass = Class.forName(className.replace(StringConstants._TRANSFORMATION_PREFIX, ""));
            return originalClass;
        } catch (Exception e) {
            throw new LabelingNotPossibleException("Original class for Mulib class of type " + className + " not found.");
        }
    }


    private ArrayConstraint[] getArrayConstraintsForSarray(Sint id) {
        int concId = _labelSintToInt(id); // TODO more performant way?
        return _getArrayConstraints().stream()
                .filter(ac -> _labelSintToInt(ac.getPartnerClassObjectId()) == concId)
                .toArray(ArrayConstraint[]::new);
    }

    private int _labelSintToInt(Sint i) {
        return ((Number) labelSprimitive(i)).intValue();
    }

    private PartnerClassObjectConstraint[] getConstraintsForPartnerClassObject(Sint id) {
        int concId = _labelSintToInt(id); // TODO more performant way?
        return _getNonArrayPartnerClassObjectConstraints().stream()
                .filter(pc -> _labelSintToInt(pc.getPartnerClassObjectId()) == concId)
                .toArray(PartnerClassObjectConstraint[]::new);
    }

    protected Object labelPartnerClassObject(PartnerClass object) {
        if (transformationRequired) {
            if (!object.__mulib__shouldBeRepresentedInSolver()) {
                Object emptyLabelObject = createEmptyLabelObject(object.__mulib__getOriginalClass());
                registerLabelPair(object, emptyLabelObject);
                Object result = object.label(
                        emptyLabelObject,
                        this
                );
                assert emptyLabelObject == result;
                return result;
            } else {
                return labelPartnerClassObject(object.__mulib__getId());
            }
        } else {
            return object;
        }
    }

    protected Object labelArray(Object array) {
        int length = Array.getLength(array);
        Object result = Array.newInstance(transformNonSarrayMulibTypeToJavaType(array.getClass().getComponentType()), length);
        registerLabelPair(array, result);
        for (int i = 0; i < length; i++) {
            Array.set(result, i, getLabel(Array.get(array, i)));
        }
        return result;
    }

    protected Object customLabelObject(Object o) {
        BiFunction<SolverManager, Object, Object> labelMethod =
                this.classesToLabelFunction.get(o.getClass());
        if (labelMethod == null) {
            registerLabelPair(o, o);
            return o;
        } else {
            Object result = labelMethod.apply(this, o);
            registerLabelPair(o, result);
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
            if (!sarray.__mulib__shouldBeRepresentedInSolver()) {
                Set<Sint> indices = sarray.getCachedIndices();
                for (Sint index : indices) {
                    SubstitutedVar cachedValue = sarray.getFromCacheForIndex(index);
                    Object label = getLabel(cachedValue);
                    disjunctionConstraint = getNeq(cachedValue, label);
                    result = Or.newInstance(result, disjunctionConstraint);
                }
            } else {
                ArrayConstraint[] acs = getArrayConstraintsForSarray(((Sarray) sv).__mulib__getId());
                for (ArrayConstraint ac : acs) {
                    if (ac instanceof ArrayInitializationConstraint) {
                        continue;
                    }
                    ArrayAccessConstraint aac = (ArrayAccessConstraint) ac;
                    SubstitutedVar val = aac.getValue();
                    Object label = getLabel(val);
                    Sint index = aac.getIndex();
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

    protected abstract AR createCompletelyNewArrayRepresentation(ArrayInitializationConstraint ac);

    protected abstract AR createNewArrayRepresentationForStore(ArrayAccessConstraint ac, AR oldRepresentation);

    protected abstract void addArraySelectConstraint(AR arrayRepresentation, Sint index, SubstitutedVar value);

    protected abstract void solverSpecificBacktrackingPoint();

    protected abstract void solverSpecificBacktrackOnce();

    protected abstract void solverSpecificBacktrack(int toBacktrack);

    protected abstract boolean calculateSatisfiabilityWithSolverBoolRepresentation(B boolExpr);

    protected abstract B newArraySelectConstraint(AR arrayRepresentation, Sint indexInArray, SubstitutedVar arrayValue);

    protected abstract B transformConstraint(Constraint c);

    protected abstract void solverSpecificShutdown();


}
