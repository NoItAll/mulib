package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.expressions.ConcolicMathematicalContainer;
import de.wwu.mulib.solving.ArrayInformation;
import de.wwu.mulib.solving.PartnerClassObjectInformation;
import de.wwu.mulib.substitutions.*;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.throwables.NotYetImplementedException;

import java.util.*;
import java.util.function.Function;

/**
 * Abstract supertype for calculation factories.
 * Implements strategies for selecting and storing from and in Sarrays and getting and putting fields in symbolic objects
 * that are reused by {@link SymbolicCalculationFactory} and {@link ConcolicCalculationFactory}.
 * It can be configured that eager indexes are used to load elements from arrays with reference-typed or primitive-typed content
 * and whether we want to throw a {@link ArrayIndexOutOfBoundsException} if a symbolic index access can be out-of-bounds.
 * An instance of {@link ValueFactory} is used to construct suiting values.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractCalculationFactory implements CalculationFactory {
    /**
     * The configuration
     */
    protected final MulibConfig config;
    /**
     * The used value factory
     */
    protected final ValueFactory valueFactory;
    private final Function<Snumber, Snumber> tryGetSymFromSnumber;
    private final Function<Sbool, Sbool> tryGetSymFromSbool;

    protected AbstractCalculationFactory(
            MulibConfig config,
            ValueFactory valueFactory,
            Function<Snumber, Snumber> tryGetSymFromSnumber,
            Function<Sbool, Sbool> tryGetSymFromSbool) {
        this.config = config;
        this.valueFactory = valueFactory;
        this.tryGetSymFromSbool = tryGetSymFromSbool;
        this.tryGetSymFromSnumber = tryGetSymFromSnumber;
    }

    @Override
    public final Sprimitive select(SymbolicExecution se, Sarray sarray, Sint index) {
        sarray.__mulib__nullCheck();
        if (config.ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS) {
            return (Sprimitive) _selectWithEagerIndexes(se, sarray, index);
        } else {
            return (Sprimitive) _selectWithSymbolicIndexes(se, sarray, index);
        }
    }

    @Override
    public final Sprimitive store(SymbolicExecution se, Sarray sarray, Sint index, Sprimitive value) {
        sarray.__mulib__nullCheck();
        if (config.ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS) {
            return (Sprimitive) _storeWithEagerIndexes(se, sarray, index, value);
        } else {
            return (Sprimitive) _storeWithSymbolicIndexes(se, sarray, index, value);
        }
    }

    @Override
    public final Sarray<?> select(SymbolicExecution se, Sarray.SarraySarray sarraySarray, Sint index) {
        sarraySarray.__mulib__nullCheck();
        if (config.ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS) {
            return (Sarray<?>) _selectWithEagerIndexes(se, sarraySarray, index);
        } else {
            return (Sarray<?>) _selectWithSymbolicIndexes(se, sarraySarray, index);
        }
    }

    @Override
    public final Sarray<?> store(SymbolicExecution se, Sarray.SarraySarray sarraySarray, Sint index, Substituted value) {
        sarraySarray.__mulib__nullCheck();
        if (config.ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS) {
            return (Sarray<?>) _storeWithEagerIndexes(se, sarraySarray, index, value);
        } else {
            return (Sarray<?>) _storeWithSymbolicIndexes(se, sarraySarray, index, value);
        }
    }

    @Override
    public final PartnerClass select(SymbolicExecution se, Sarray.PartnerClassSarray<?> partnerClassSarray, Sint index) {
        partnerClassSarray.__mulib__nullCheck();
        if (config.ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS) {
            return (PartnerClass) _selectWithEagerIndexes(se, partnerClassSarray, index);
        } else {
            return (PartnerClass) _selectWithSymbolicIndexes(se, partnerClassSarray, index);
        }
    }

    @Override
    public final PartnerClass store(SymbolicExecution se, Sarray.PartnerClassSarray<?> partnerClassSarray, Sint index, Substituted value) {
        partnerClassSarray.__mulib__nullCheck();
        if (config.ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS) {
            return (PartnerClass) _storeWithEagerIndexes(se, partnerClassSarray, index, value);
        } else {
            return (PartnerClass) _storeWithSymbolicIndexes(se, partnerClassSarray, index, value);
        }
    }

    // Inspired by https://github.com/SymbolicPathFinder/jpf-symbc/blob/046eb3c3029583a8326714c69fbdef7c56c2690b/src/main/gov/nasa/jpf/symbc/bytecode/symarrays/AALOAD.java
    // Eagerly decide on which index to choose
    private Substituted _selectWithEagerIndexes(SymbolicExecution se, Sarray sarray, Sint index) {
        Substituted result = sarray.getFromCacheForIndex(index);
        if (result != null) {
            return result;
        }

        checkIndexAccess(sarray, index, se);
        Sint concsIndex = decideOnConcreteIndex(se, index);
        result = sarray.getFromCacheForIndex(concsIndex);
        if (result != null) {
            sarray.setInCacheForIndex(index, result);
            return result;
        }
        result = sarray.getNewValueForSelect(se, index);
        sarray.setInCacheForIndex(concsIndex, result);
        sarray.setInCacheForIndex(index, result);
        return result;
    }

    private Substituted _storeWithEagerIndexes(SymbolicExecution se, Sarray sarray, Sint index, Substituted value) {
        checkIndexAccess(sarray, index, se);
        Sarray.checkIfValueIsStorableForSarray(sarray, value);
        Sint concsIndex = decideOnConcreteIndex(se, index);
        // We can simply use setInCacheForIndexForSelect since for this approach. There won't be any representation of
        // this array in the constraint solver, hence, aliasing is not an issue. This is the case since we forbid the use
        // of eager indexes for arrays with primitive elements if this is not also enabled for arrays with object element
        // types
        sarray.setInCacheForIndex(concsIndex, value);
        sarray.setInCacheForIndex(index, value);
        return value;
    }

    private static Sint decideOnConcreteIndex(SymbolicExecution se, Sint index) {
        if (index instanceof ConcSnumber) {
            return index;
        }
        Sint concsIndex;
        int currentIntIndex = 0;
        while (true) {
            Sint currentIndex = se.concSint(currentIntIndex);
            if (se.eqChoice(index, currentIndex)) {
                concsIndex = currentIndex;
                break;
            }
            currentIntIndex++;
        }
        return concsIndex;
    }

    private void checkIndexAccess(Sarray sarray, Sint i, SymbolicExecution se) {
        if (i instanceof ConcSnumber && ((ConcSnumber) i).intVal() < 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        // Use sarray.getLength to also check for null
        if (sarray.length() instanceof Sint.SymSint || i instanceof Sint.SymSint) {
            // If either the length or the index are symbolic, there can potentially be an
            // ArrayIndexOutOfBoundsException.
            Sbool indexInBound = se.and(
                    se.lt(i, sarray._getLengthWithoutCheckingForIsNull()),
                    se.lte(Sint.ConcSint.ZERO, i)
            );
            if (config.ARRAYS_THROW_EXCEPTION_ON_OOB) {
                boolean inBounds = se.boolChoice(indexInBound);
                if (!inBounds) {
                    throw new ArrayIndexOutOfBoundsException();
                }
            } else if (!se.nextIsOnKnownPath()) {
                // If we do not regard out-of-bound array index-accesses, we simply add a new constraint and proceed.
                //  next choice option or once reaching the end of the execution. Find an approach with minimal overhead
                //  here.
                _addIndexInBoundsConstraint(se, indexInBound);
            }
        } else {
            ConcSnumber concLen = (ConcSnumber) sarray._getLengthWithoutCheckingForIsNull();
            ConcSnumber concI = (ConcSnumber) i;
            if (concLen.intVal() <= concI.intVal() || concI.intVal() < 0) {
                throw new ArrayIndexOutOfBoundsException();
            }
        }
    }


    private void addSelectConstraintIfNeeded(SymbolicExecution se, Sarray sarray, Sint index, Substituted result) {
        // We will now add a constraint indicating to the solver that at position i a value can be found that previously
        // was not there. This only occurs if the array must be represented via constraints. This, in turn, only
        // is the case if symbolic indices have been used.
        if (sarray.__mulib__isRepresentedInSolver()) {
            if (result instanceof PartnerClass) {
                assert sarray instanceof Sarray.PartnerClassSarray;
                representPartnerClassObjectIfNeeded(se, (PartnerClass) result, sarray.__mulib__getId(), null, index);
            }
            if (!se.nextIsOnKnownPath()) {
                Sprimitive val = getValueToBeUsedForPartnerClassObjectConstraint(result);
                ArrayConstraint selectConstraint =
                        new ArrayAccessConstraint(
                                (Sint) tryGetSymFromSnumber.apply(sarray.__mulib__getId()),
                                (Sint) tryGetSymFromSnumber.apply(index),
                                val,
                                ArrayAccessConstraint.Type.SELECT
                        );
                se.addNewPartnerClassObjectConstraint(selectConstraint);
            }
        }
    }

    /**
     * Used to get the value to represent for the constraint solver. If the value is a {@link PartnerClass},
     * the identifier is returned. A pure value must be returned, i.e., wrapping such as {@link ConcolicMathematicalContainer}
     * or {@link ConcolicConstraintContainer} must be unpacked.
     * @param value The value
     * @return The Sprimitive representing the value
     */
    protected abstract Sprimitive getValueToBeUsedForPartnerClassObjectConstraint(Substituted value);

    @Override
    public void initializeLazyFields(SymbolicExecution se, PartnerClass pco) {
        assert pco.__mulib__defaultIsSymbolic();
        assert !pco.__mulib__isLazilyInitialized();
        assert !(pco instanceof Sarray);
        // If pco is representedInTheSolver, we get its field values from the SolverManager anyways
        if (pco.__mulib__isToBeLazilyInitialized()) {
            pco.__mulib__initializeLazyFields(se);
        }
        pco.__mulib__setAsLazilyInitialized();
    }

    @Override
    public Substituted getField(SymbolicExecution se, PartnerClass pco, String field, Class<?> fieldClass) {
        assert fieldClass != null;
        assert pco.__mulib__isNull() == Sbool.ConcSbool.FALSE;
        assert pco.__mulib__isRepresentedInSolver();
        // Initialize value that is to be retrieved from a field
        Substituted fieldValue = getSymValueForFieldInRepresentedPartnerClassObject(se, pco, field, fieldClass);
        if (fieldValue instanceof PartnerClass) {
            PartnerClass pcval = (PartnerClass) fieldValue;
            representPartnerClassObjectIfNeeded(se, pcval, pco.__mulib__getId(), field, null);
        }
        if (!se.nextIsOnKnownPath()) {
            Sprimitive val = getValueToBeUsedForPartnerClassObjectConstraint(fieldValue);
            se.addNewPartnerClassObjectConstraint(
                    new PartnerClassObjectFieldConstraint(
                            (Sint) tryGetSymFromSnumber.apply(pco.__mulib__getId()),
                            field,
                            val,
                            PartnerClassObjectFieldConstraint.Type.GETFIELD
                    )
            );
        }
        return fieldValue;
    }

    @Override
    public void putField(SymbolicExecution se, PartnerClass pco, String field, Substituted value) {
        assert pco.__mulib__isNull() == Sbool.ConcSbool.FALSE;
        assert pco.__mulib__isRepresentedInSolver();
        assert !(pco instanceof Sarray);
        if (value instanceof PartnerClass) {
            PartnerClass pcVal = (PartnerClass) value;
            if (!pcVal.__mulib__isRepresentedInSolver()) {
                PartnerClass pcval = (PartnerClass) value;
                pcval.__mulib__prepareToRepresentSymbolically(se);
            }
            representPartnerClassObjectIfNeeded(se, pcVal, pco.__mulib__getId(), field, null);
        }
        if (!se.nextIsOnKnownPath()) {
            Sprimitive val = getValueToBeUsedForPartnerClassObjectConstraint(value);
            se.addNewPartnerClassObjectConstraint(
                    new PartnerClassObjectFieldConstraint(
                            (Sint) tryGetSymFromSnumber.apply(pco.__mulib__getId()),
                            field,
                            val,
                            PartnerClassObjectFieldConstraint.Type.PUTFIELD
                    )
            );
        }
    }

    private void _generateIdIfNeeded(
            SymbolicExecution se,
            PartnerClass pco,
            Substituted value) {
        assert !(pco instanceof Sarray);
        assert pco.__mulib__isRepresentedInSolver();
        if (value instanceof PartnerClass) {
            PartnerClass pcval = (PartnerClass) value;
            if (pcval.__mulib__getId() == null) {
                if (pco.__mulib__getId() instanceof SymSnumber) {
                    pcval.__mulib__prepareForAliasingAndBlockCache(se);
                } else {
                    pcval.__mulib__prepareToRepresentSymbolically(se);
                }
            }
        }
    }

    private Substituted getSymValueForFieldInRepresentedPartnerClassObject(SymbolicExecution se, PartnerClass pco, String field, Class<?> fieldClass) {
        assert pco.__mulib__isRepresentedInSolver();
        Substituted fieldValue;
        if (Sprimitive.class.isAssignableFrom(fieldClass)) {
            fieldValue = getSymValueForSprimitiveClass(se, fieldClass);
        } else if (fieldClass.isArray() || PartnerClass.class.isAssignableFrom(fieldClass)) {
            boolean canBeNull;
            boolean defaultIsSymbolic;
            // Determine nullability:
            if (!se.nextIsOnKnownPath()) {
                // If this is a new choice option, we discover nullability by looking at the containing object
                // at its current state
                PartnerClassObjectInformation pcoi =
                        getAvailableInformationOnPartnerClassObject(
                                se,
                                pco,
                                field
                        );
                canBeNull = pcoi.fieldCanPotentiallyContainNull;
            } else {
                // For now, we assume that the object cannot be null
                // It is not represented in the solver from here anyway, so this
                // does not alter the trail of PartnerClassObjectConstraints
                // In the code block guarded bei se.nextIsOnKnownPath() this is rectified
                canBeNull = false;
            }
            defaultIsSymbolic = pco.__mulib__defaultIsSymbolic();
            PartnerClass fieldValuePco = getSymValueForPartnerClassClass(se, defaultIsSymbolic, canBeNull, fieldClass);

            if (!fieldValuePco.__mulib__isRepresentedInSolver()) {
                fieldValuePco.__mulib__prepareForAliasingAndBlockCache(se);
            }

            if (se.nextIsOnKnownPath()) {
                // If this is not a new choice option, we determine nullability by looking at the representation of the object
                // for the solver that was already initialized via a trail
                // We need to do this since the object might be altered since the first time, it was seen. For instance,
                // a null might be stored in a field directly after accessing this current field
                Sbool nullVal;
                assert !Sarray.class.isAssignableFrom(fieldClass);
                if (fieldClass.isArray()) {
                    nullVal = getAvailableInformationOnArray(se, (Sarray) fieldValuePco).isNull;
                } else {
                    nullVal = getAvailableInformationOnPartnerClassObject(se, fieldValuePco, /* No info on fields is required */null).isNull;
                }
                canBeNull = nullVal instanceof Sbool.SymSbool || ((Sbool.ConcSbool) nullVal).isTrue();
                if (canBeNull) {
                    Sbool isNull = se.symSbool();
                    // Now we actually set isNull to its value
                    fieldValuePco.__mulib__setIsNull(isNull);
                    // Check whether the trail has been correctly set
                    assert nullVal == tryGetSymFromSbool.apply(isNull) : "We assume that no symSbool is taken in between the initialization";
                }
            }
            fieldValue = fieldValuePco;
        } else {
            throw new NotYetImplementedException();
        }
        return fieldValue;
    }

    private Sprimitive getSymValueForSprimitiveClass(SymbolicExecution se, Class<?> clazz) {
        assert Sprimitive.class.isAssignableFrom(clazz);
        Sprimitive fieldValue;
        if (Sint.class.isAssignableFrom(clazz)) { // TODO get value more efficiently
            if (Sbool.class.isAssignableFrom(clazz)) {
                fieldValue = se.symSbool();
            } else if (Sbyte.class.isAssignableFrom(clazz)) {
                fieldValue = se.symSbyte();
            } else if (Sshort.class.isAssignableFrom(clazz)) {
                fieldValue = se.symSshort();
            } else if (Schar.class.isAssignableFrom(clazz)) {
                fieldValue = se.symSchar();
            } else {
                assert clazz == Sint.class;
                fieldValue = se.symSint();
            }
        } else if (Sdouble.class.isAssignableFrom(clazz)) {
            fieldValue = se.symSdouble();
        } else if (Slong.class.isAssignableFrom(clazz)) {
            fieldValue = se.symSlong();
        } else if (Sfloat.class.isAssignableFrom(clazz)) {
            fieldValue = se.symSfloat();
        } else {
            throw new NotYetImplementedException(clazz.toString());
        }
        return fieldValue;
    }

    private PartnerClass getSymValueForPartnerClassClass(SymbolicExecution se, boolean defaultIsSymbolic, boolean canBeNull, Class<?> clazz) {
        assert !Sarray.class.isAssignableFrom(clazz);
        PartnerClass fieldValue;
        // TODO refactor with SarraySarray
        if (clazz.isArray()) {
            Sint len = se.symSint();
            Class<?> fieldClassComponentType = clazz.getComponentType();
            if (clazz.getComponentType().isArray()) {
                fieldValue = se.sarraySarray(len, fieldClassComponentType, defaultIsSymbolic, canBeNull);
            } else if (Sprimitive.class.isAssignableFrom(fieldClassComponentType)) {
                if (fieldClassComponentType == Sint.class) {
                    fieldValue = se.sintSarray(len, defaultIsSymbolic, canBeNull);
                } else if (fieldClassComponentType == Slong.class) {
                    fieldValue = se.slongSarray(len, defaultIsSymbolic, canBeNull);
                } else if (fieldClassComponentType == Sdouble.class) {
                    fieldValue = se.sdoubleSarray(len, defaultIsSymbolic, canBeNull);
                } else if (fieldClassComponentType == Sfloat.class) {
                    fieldValue = se.sfloatSarray(len, defaultIsSymbolic, canBeNull);
                } else if (fieldClassComponentType == Sshort.class) {
                    fieldValue = se.sshortSarray(len, defaultIsSymbolic, canBeNull);
                } else if (fieldClassComponentType == Sbyte.class) {
                    fieldValue = se.sbyteSarray(len, defaultIsSymbolic, canBeNull);
                } else if (fieldClassComponentType == Sbool.class) {
                    fieldValue = se.sboolSarray(len, defaultIsSymbolic, canBeNull);
                } else {
                    throw new NotYetImplementedException();
                }
            } else if (PartnerClass.class.isAssignableFrom(fieldClassComponentType)) {
                fieldValue = se.partnerClassSarray(
                        len,
                        (Class<? extends PartnerClass>) fieldClassComponentType,
                        defaultIsSymbolic,
                        canBeNull
                );
            } else {
                throw new NotYetImplementedException(clazz.getTypeName());
            }
        } else {
            fieldValue = se.symObject((Class<PartnerClass>) clazz);
            fieldValue.__mulib__setIsNull(canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
        }
        return fieldValue;
    }

    @Override
    public void representPartnerClassObjectIfNeeded(
            SymbolicExecution se,
            PartnerClass toPotentiallyRepresent,
            Sint idOfContainingPartnerClassObject,
            String fieldName,
            Sint index) {
        assert fieldName == null || index == null;
        if (!toPotentiallyRepresent.__mulib__shouldBeRepresentedInSolver() || toPotentiallyRepresent.__mulib__isRepresentedInSolver()) {
            return;
        }
        assert !(toPotentiallyRepresent instanceof Sarray) || ((Sarray) toPotentiallyRepresent).getCachedIndices().stream().noneMatch(i -> i instanceof Sym)
                : "The Sarray should have already been represented in the constraint system";
        assert toPotentiallyRepresent.__mulib__getId() != null;
        assert !(toPotentiallyRepresent.__mulib__getId() instanceof Sym) || idOfContainingPartnerClassObject != null;
        // Already set this so that we do not have any issues with circular dependencies
        toPotentiallyRepresent.__mulib__setAsRepresentedInSolver();
        // If needed, represent complex-typed entries in solver
        if (toPotentiallyRepresent instanceof Sarray.PartnerClassSarray) {
            // Includes SarraySarrays
            for (Sint i : ((Sarray<?>) toPotentiallyRepresent).getCachedIndices()) {
                Object objectEntry = ((Sarray.PartnerClassSarray<?>) toPotentiallyRepresent).getFromCacheForIndex(i);
                if (objectEntry == null) {
                    continue;
                }
                PartnerClass entry = (PartnerClass) objectEntry;
                if (!entry.__mulib__isRepresentedInSolver()) {
                    assert !entry.__mulib__shouldBeRepresentedInSolver();
                    entry.__mulib__prepareToRepresentSymbolically(se);
                    // Is a Sarray, i.e., we do not have a field here
                    representPartnerClassObjectIfNeeded(se, entry, toPotentiallyRepresent.__mulib__getId(), null, i);
                }
                if (entry instanceof Sarray) {
                    assert entry.__mulib__cacheIsBlocked();
                    ((Sarray<?>) entry).clearCache();
                }
            }
        } else if (!(toPotentiallyRepresent instanceof Sarray)) {
            // Check the fields of 'real' PartnerClass objects
            // We must not initialize the fields, if they have not already been initialized
            Map<String, Substituted> fieldValues = toPotentiallyRepresent.__mulib__getFieldNameToSubstituted();
            for (Map.Entry<String, Substituted> entry : fieldValues.entrySet()) {
                Substituted val = entry.getValue();
                if (!(val instanceof PartnerClass)) {
                    // This stops both null-values as well as primitive values. We only
                    // initialize non-null-values
                    continue;
                }
                PartnerClass pc = (PartnerClass) val;
                _generateIdIfNeeded(se, toPotentiallyRepresent, pc);
                representPartnerClassObjectIfNeeded(se, pc, toPotentiallyRepresent.__mulib__getId(), entry.getKey(), null);
            }
        }
        // Represent array OR partnerclass object in constraint solver, if needed
        if (toPotentiallyRepresent.__mulib__getId() instanceof ConcSnumber) {
            // In this case the PartnerClass object was not spawned from a select
            // from a PartnerClassSarray or a PartnerClass object
            if (!se.nextIsOnKnownPath()) {
                if (toPotentiallyRepresent instanceof Sarray) {
                    Sarray sarray = (Sarray) toPotentiallyRepresent;
                    ArrayInitializationConstraint arrayInitializationConstraint = new ArrayInitializationConstraint(
                            (Sint) tryGetSymFromSnumber.apply(sarray.__mulib__getId()),
                            (Sint) tryGetSymFromSnumber.apply(sarray._getLengthWithoutCheckingForIsNull()),
                            tryGetSymFromSbool.apply(sarray.__mulib__isNull()),
                            sarray.getElementType(),
                            collectInitialArrayAccessConstraints(sarray, se),
                            sarray.__mulib__defaultIsSymbolic()
                    );
                    assert arrayInitializationConstraint.getType() == ArrayInitializationConstraint.Type.SIMPLE_SARRAY;
                    se.addNewPartnerClassObjectConstraint(arrayInitializationConstraint);
                } else {
                    assert toPotentiallyRepresent.__mulib__getId() != null;
                    PartnerClassObjectInitializationConstraint c =
                            new PartnerClassObjectInitializationConstraint(
                                    toPotentiallyRepresent.getClass(),
                                    (Sint) tryGetSymFromSnumber.apply(toPotentiallyRepresent.__mulib__getId()),
                                    tryGetSymFromSbool.apply(toPotentiallyRepresent.__mulib__isNull()),
                                    collectInitialPartnerClassObjectFieldConstraints(toPotentiallyRepresent, se),
                                    toPotentiallyRepresent.__mulib__defaultIsSymbolic()
                            );
                    assert c.getType() == PartnerClassObjectInitializationConstraint.Type.SIMPLE_PARTNER_CLASS_OBJECT;
                    se.addNewPartnerClassObjectConstraint(c);
                }
            }
        } else {
            // Id is symbolic
            // In this case we must initialize the Sarray with the possibility of aliasing the elements in the sarray
            Sint reservedId = se.concSint(se.getNextNumberInitializedSymObject());
            if (!se.nextIsOnKnownPath()) {
                PartnerClassObjectConstraint initializationConstraint;
                if (toPotentiallyRepresent instanceof Sarray) {
                    Sarray sarray = (Sarray) toPotentiallyRepresent;
                    initializationConstraint = new ArrayInitializationConstraint(
                            (Sint) tryGetSymFromSnumber.apply(sarray.__mulib__getId()),
                            (Sint) tryGetSymFromSnumber.apply(sarray._getLengthWithoutCheckingForIsNull()),
                            tryGetSymFromSbool.apply(sarray.__mulib__isNull()),
                            // Id reserved for this Sarray, if needed
                            reservedId,
                            (Sint) tryGetSymFromSnumber.apply(idOfContainingPartnerClassObject),
                            fieldName, // Is null if container is sarray
                            (Sint) tryGetSymFromSnumber.apply(index), // Is null if container is partner class object
                            sarray.getElementType(),
                            collectInitialArrayAccessConstraints(sarray, se),
                            sarray.__mulib__defaultIsSymbolic()
                    );
                    assert ((ArrayInitializationConstraint) initializationConstraint).getType() ==
                            (fieldName == null ?
                                    ArrayInitializationConstraint.Type.SARRAY_IN_SARRAY
                                    :
                                    ArrayInitializationConstraint.Type.SARRAY_IN_PARTNER_CLASS_OBJECT);
                } else {
                    initializationConstraint =
                            new PartnerClassObjectInitializationConstraint(
                                    toPotentiallyRepresent.getClass(),
                                    (Sint) tryGetSymFromSnumber.apply(toPotentiallyRepresent.__mulib__getId()),
                                    tryGetSymFromSbool.apply(toPotentiallyRepresent.__mulib__isNull()),
                                    reservedId,
                                    (Sint) tryGetSymFromSnumber.apply(idOfContainingPartnerClassObject),
                                    fieldName, // Is null if container is sarray
                                    (Sint) tryGetSymFromSnumber.apply(index), // Is null if container is partner class object
                                    collectInitialPartnerClassObjectFieldConstraints(toPotentiallyRepresent, se),
                                    toPotentiallyRepresent.__mulib__defaultIsSymbolic()
                            );
                    assert ((PartnerClassObjectInitializationConstraint) initializationConstraint).getType() ==
                            (fieldName == null ?
                                    PartnerClassObjectInitializationConstraint.Type.PARTNER_CLASS_OBJECT_IN_SARRAY
                                    :
                                    PartnerClassObjectInitializationConstraint.Type.PARTNER_CLASS_OBJECT_IN_PARTNER_CLASS_OBJECT);
                }
                se.addNewPartnerClassObjectConstraint(initializationConstraint);
            }
        }

        if (!(toPotentiallyRepresent instanceof Sarray)) {
            toPotentiallyRepresent.__mulib__blockCache();
        }

        if (toPotentiallyRepresent instanceof Sarray) {
            ((Sarray<?>) toPotentiallyRepresent).clearCache();
        }
    }

    private void representPartnerClassObjectViaConstraintsIfNeeded(SymbolicExecution se, PartnerClass ihsv, boolean additionalConstraintToPrepare) {
        if (!ihsv.__mulib__isRepresentedInSolver() && additionalConstraintToPrepare) {
            ihsv.__mulib__prepareToRepresentSymbolically(se);
        }
        representPartnerClassObjectIfNeeded(se, ihsv, null, null, null);
    }

    @Override
    public PartnerClassObjectInformation getAvailableInformationOnPartnerClassObject(SymbolicExecution se, PartnerClass containingObject, String field) {
        assert !(containingObject instanceof Sarray);
        return se.getAvailableInformationOnPartnerClassObject((Sint) tryGetSymFromSnumber.apply(containingObject.__mulib__getId()), field);
    }

    @Override
    public ArrayInformation getAvailableInformationOnArray(SymbolicExecution se, Sarray var) {
        return se.getAvailableInformationOnArray((Sint) tryGetSymFromSnumber.apply(var.__mulib__getId()));
    }

    private PartnerClassObjectFieldConstraint[] collectInitialPartnerClassObjectFieldConstraints(PartnerClass pc, SymbolicExecution se) {
        assert !se.nextIsOnKnownPath() && pc.__mulib__shouldBeRepresentedInSolver() && pc.__mulib__isRepresentedInSolver();
        Map<String, Substituted> fieldsToValues = pc.__mulib__getFieldNameToSubstituted();
        List<PartnerClassObjectFieldConstraint> result = new ArrayList<>();
        for (Map.Entry<String, Substituted> e : fieldsToValues.entrySet()) {
            Sprimitive value = getValueToBeUsedForPartnerClassObjectConstraint(e.getValue());
            assert !(e.getValue() instanceof PartnerClass) || ((PartnerClass) e.getValue()).__mulib__isRepresentedInSolver();
            if (e.getValue() == null && pc.__mulib__isSymbolicAndNotYetLazilyInitialized()) {
                continue;
            }
            result.add(new PartnerClassObjectFieldConstraint(
                    pc.__mulib__getId(),
                    e.getKey(),
                    value,
                    PartnerClassObjectFieldConstraint.Type.GETFIELD
            ));
        }
        return result.toArray(PartnerClassObjectFieldConstraint[]::new);
    }

    @Override
    public void remember(SymbolicExecution se, String name, Substituted substituted) {
        if (substituted instanceof PartnerClass) {
            PartnerClass pc = (PartnerClass) substituted;
            pc.__mulib__setIsRemembered();
            // TODO Another remember-method should take a whole set of SubstitutedVars with their names to remember
            //  The benefit would be that they all recognize object identity as they come from the same MulibValueCopier
            MulibValueCopier mulibValueCopier = new MulibValueCopier(se, config);
            if (pc.__mulib__isToBeLazilyInitialized()) {
                pc.__mulib__prepareToRepresentSymbolically(se);
                representPartnerClassObjectIfNeeded(se, pc, null, null, null);
            } else {
                // This must also be done for non-symbolic objects, since they might contain uninitialized lazily generated objects
                _initializeIdsOfContainedLazilyInitializedObjects(se, pc);
            }
            if (!se.nextIsOnKnownPath()) {
                PartnerClass copied = (PartnerClass) mulibValueCopier.copyNonSprimitive(pc);
                PartnerClassObjectConstraint rememberConstraint =
                        new PartnerClassObjectRememberConstraint(name, copied);
                se.addNewPartnerClassObjectConstraint(rememberConstraint);
            }
        }
    }

    private static void _initializeIdsOfContainedLazilyInitializedObjects(SymbolicExecution se, PartnerClass container) {
        ArrayDeque<PartnerClass> toEvaluate = new ArrayDeque<>();
        toEvaluate.add(container);
        List<PartnerClass> alreadyEvaluated = new ArrayList<>();
        while (!toEvaluate.isEmpty()) {
            PartnerClass current = toEvaluate.poll();
            if (alreadyEvaluated.contains(current)) {
                continue;
            }
            alreadyEvaluated.add(current);
            Collection<Substituted> vals = current instanceof Sarray ? (Collection<Substituted>) ((Sarray<?>) current).getCachedElements() : current.__mulib__getFieldNameToSubstituted().values();
            for (Substituted entry : vals) {
                if (!(entry instanceof PartnerClass)) {
                    continue;
                }
                PartnerClass pc = (PartnerClass) entry;
                if (pc.__mulib__defaultIsSymbolic()
                        && (!(pc instanceof Sarray) || ((Sarray<?>) pc)._getLengthWithoutCheckingForIsNull() instanceof Sym)
                        && !pc.__mulib__isRepresentedInSolver()) {
                    pc.__mulib__prepareToRepresentSymbolically(se);
                    se.getCalculationFactory().representPartnerClassObjectIfNeeded(se, pc, null, null, null);
                } else {
                    toEvaluate.add(pc);
                }
            }
        }
    }

    private ArrayAccessConstraint[] collectInitialArrayAccessConstraints(Sarray sarray, SymbolicExecution se) {
        assert !se.nextIsOnKnownPath() && sarray.__mulib__shouldBeRepresentedInSolver();
        Set<Sint> cachedIndices = sarray.getCachedIndices();
        assert cachedIndices.stream().noneMatch(i -> i instanceof Sym) : "The Sarray should have already been represented in the constraint system";

        List<ArrayAccessConstraint> initialConstraints = new ArrayList<>();
        for (Sint i : cachedIndices) {
            Substituted value = sarray.getFromCacheForIndex(i);
            assert !(value instanceof PartnerClass)
                    // Value was represented beforehand
                    || ((PartnerClass) value).__mulib__isRepresentedInSolver();
            Sprimitive val = getValueToBeUsedForPartnerClassObjectConstraint(value);
            ArrayAccessConstraint ac = new ArrayAccessConstraint(
                    (Sint) tryGetSymFromSnumber.apply(sarray.__mulib__getId()),
                    (Sint) tryGetSymFromSnumber.apply(i),
                    val,
                    ArrayAccessConstraint.Type.SELECT
            );
            initialConstraints.add(ac);
        }
        return initialConstraints.toArray(new ArrayAccessConstraint[0]);
    }

    /**
     * Adds a constraint that ensured that a value is in the bounds of an array.
     * Is used to account for the case where concolic execution finds that we must relabel the values.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param indexInBounds The constraint
     */
    protected abstract void _addIndexInBoundsConstraint(SymbolicExecution se, Sbool indexInBounds);

    private Substituted _selectWithSymbolicIndexes(SymbolicExecution se, Sarray sarray, Sint index) {
        Substituted result;
        if (!sarray.__mulib__isRepresentedInSolver()) {
            result = sarray.getFromCacheForIndex(index);
            if (result != null) {
                // Shortcut: SintSarray etc. should never contain nulls
                return result;
            } else if (sarray.getCachedIndices().contains(index)) {
                assert sarray instanceof Sarray.PartnerClassSarray;
                // If the index is known to store null, return null
                return null;
            }
        }

        representPartnerClassObjectViaConstraintsIfNeeded(se, sarray, index instanceof Sym || sarray._getLengthWithoutCheckingForIsNull() instanceof Sym);
        checkIndexAccess(sarray, index, se);

        result = sarray.getNewValueForSelect(se, index);
        addSelectConstraintIfNeeded(se, sarray, index, result);
        sarray.setInCacheForIndex(index, result);
        return result;
    }

    private Substituted _storeWithSymbolicIndexes(SymbolicExecution se, Sarray sarray, Sint index, Substituted value) {
        representPartnerClassObjectViaConstraintsIfNeeded(se, sarray,  index instanceof Sym || sarray._getLengthWithoutCheckingForIsNull() instanceof Sym);
        checkIndexAccess(sarray, index, se);
        Sarray.checkIfValueIsStorableForSarray(sarray, value);

        // Similarly to select, we will notify the solver, if needed, that the representation of the array has changed.
        if (sarray.__mulib__isRepresentedInSolver()) {
            if (value instanceof PartnerClass) {
                representPartnerClassObjectViaConstraintsIfNeeded(se, (PartnerClass) value, true);
            }
            if (!se.nextIsOnKnownPath()) {
                Sprimitive val = getValueToBeUsedForPartnerClassObjectConstraint(value);
                ArrayConstraint storeConstraint =
                        new ArrayAccessConstraint(
                                (Sint) tryGetSymFromSnumber.apply(sarray.__mulib__getId()),
                                (Sint) tryGetSymFromSnumber.apply(index),
                                val,
                                ArrayAccessConstraint.Type.STORE
                        );
                se.addNewPartnerClassObjectConstraint(storeConstraint);
            }
        }
        sarray.setInCacheForIndex(index, value);
        return value;
    }
}
