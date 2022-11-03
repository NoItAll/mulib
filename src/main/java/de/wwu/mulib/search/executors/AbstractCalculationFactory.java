package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.solving.ArrayInformation;
import de.wwu.mulib.solving.PartnerClassObjectInformation;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractCalculationFactory implements CalculationFactory {
    protected final MulibConfig config;
    protected final boolean throwExceptionOnOOB;
    protected final boolean useEagerIndexesForFreeArrayPrimitiveElements;
    protected final boolean useEagerIndexesForFreeArrayObjectElements;
    protected final boolean enableInitializeFreeArraysWithNull;
    protected final ValueFactory valueFactory;
    protected final Function<Snumber, Snumber> tryGetSymFromSnumber;
    protected final Function<Sbool, Sbool> tryGetSymFromSbool;

    protected AbstractCalculationFactory(
            MulibConfig config,
            ValueFactory valueFactory,
            Function<Snumber, Snumber> tryGetSymFromSnumber,
            Function<Sbool, Sbool> tryGetSymFromSbool) {
        this.config = config;
        this.throwExceptionOnOOB = config.THROW_EXCEPTION_ON_OOB;
        this.useEagerIndexesForFreeArrayPrimitiveElements = config.USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS;
        this.useEagerIndexesForFreeArrayObjectElements = config.USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS;
        this.enableInitializeFreeArraysWithNull = config.ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL;
        this.valueFactory = valueFactory;
        this.tryGetSymFromSbool = tryGetSymFromSbool;
        this.tryGetSymFromSnumber = tryGetSymFromSnumber;
    }

    @Override
    public final Sprimitive select(SymbolicExecution se, Sarray sarray, Sint index) {
        sarray.__mulib__nullCheck();
        if (useEagerIndexesForFreeArrayPrimitiveElements) {
            return (Sprimitive) _selectWithEagerIndexes(se, sarray, index);
        } else {
            return (Sprimitive) _selectWithSymbolicIndexes(se, sarray, index);
        }
    }

    @Override
    public final Sprimitive store(SymbolicExecution se, Sarray sarray, Sint index, Sprimitive value) {
        sarray.__mulib__nullCheck();
        if (useEagerIndexesForFreeArrayPrimitiveElements) {
            return (Sprimitive) _storeWithEagerIndexes(se, sarray, index, value);
        } else {
            return (Sprimitive) _storeWithSymbolicIndexes(se, sarray, index, value);
        }
    }

    @Override
    public final Sarray<?> select(SymbolicExecution se, Sarray.SarraySarray sarraySarray, Sint index) {
        sarraySarray.__mulib__nullCheck();
        if (useEagerIndexesForFreeArrayObjectElements) {
            return (Sarray<?>) _selectWithEagerIndexes(se, sarraySarray, index);
        } else {
            return (Sarray<?>) _selectWithSymbolicIndexes(se, sarraySarray, index);
        }
    }

    @Override
    public final Sarray<?> store(SymbolicExecution se, Sarray.SarraySarray sarraySarray, Sint index, SubstitutedVar value) {
        sarraySarray.__mulib__nullCheck();
        if (useEagerIndexesForFreeArrayObjectElements) {
            return (Sarray<?>) _storeWithEagerIndexes(se, sarraySarray, index, value);
        } else {
            return (Sarray<?>) _storeWithSymbolicIndexes(se, sarraySarray, index, value);
        }
    }

    @Override
    public final PartnerClass select(SymbolicExecution se, Sarray.PartnerClassSarray<?> partnerClassSarray, Sint index) {
        partnerClassSarray.__mulib__nullCheck();
        if (useEagerIndexesForFreeArrayObjectElements) {
            return (PartnerClass) _selectWithEagerIndexes(se, partnerClassSarray, index);
        } else {
            return (PartnerClass) _selectWithSymbolicIndexes(se, partnerClassSarray, index);
        }
    }

    @Override
    public final PartnerClass store(SymbolicExecution se, Sarray.PartnerClassSarray<?> partnerClassSarray, Sint index, SubstitutedVar value) {
        partnerClassSarray.__mulib__nullCheck();
        if (useEagerIndexesForFreeArrayObjectElements) {
            return (PartnerClass) _storeWithEagerIndexes(se, partnerClassSarray, index, value);
        } else {
            return (PartnerClass) _storeWithSymbolicIndexes(se, partnerClassSarray, index, value);
        }
    }

    // Inspired by https://github.com/SymbolicPathFinder/jpf-symbc/blob/046eb3c3029583a8326714c69fbdef7c56c2690b/src/main/gov/nasa/jpf/symbc/bytecode/symarrays/AALOAD.java
    // Eagerly decide on which index to choose
    private SubstitutedVar _selectWithEagerIndexes(SymbolicExecution se, Sarray sarray, Sint index) {
        SubstitutedVar result = sarray.getFromCacheForIndex(index);
        if (result != null) {
            return result;
        }

        checkIndexAccess(sarray, index, se);
        Sint concsIndex = decideOnConcreteIndex(se, index);
        result = sarray.getFromCacheForIndex(concsIndex);
        if (result != null) {
            sarray.setInCacheForIndexForSelect(index, result);
            return result;
        }
        result = sarray.getNewValueForSelect(se);
        sarray.setInCacheForIndexForSelect(concsIndex, result);
        sarray.setInCacheForIndexForSelect(index, result);
        return result;
    }

    private SubstitutedVar _storeWithEagerIndexes(SymbolicExecution se, Sarray sarray, Sint index, SubstitutedVar value) {
        checkIndexAccess(sarray, index, se);
        Sarray.checkIfValueIsStorableForSarray(sarray, value);
        Sint concsIndex = decideOnConcreteIndex(se, index);
        // We can simply use setInCacheForIndexForSelect since for this approach. There won't be any representation of
        // this array in the constraint solver, hence, aliasing is not an issue. This is the case since we forbid the use
        // of eager indexes for arrays with primitive elements if this is not also enabled for arrays with object element
        // types
        sarray.setInCacheForIndexForSelect(concsIndex, value);
        sarray.setInCacheForIndexForSelect(index, value);
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

    protected void checkIndexAccess(Sarray sarray, Sint i, SymbolicExecution se) {
        if (i instanceof ConcSnumber && ((ConcSnumber) i).intVal() < 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        // Use sarray.getLength to also check for null
        if (sarray.getLength() instanceof Sint.SymSint || i instanceof Sint.SymSint) {
            // If either the length or the index are symbolic, there can potentially be an
            // ArrayIndexOutOfBoundsException.
            Sbool indexInBound = se.and(
                    se.lt(i, sarray._getLengthWithoutCheckingForIsNull()),
                    se.lte(Sint.ConcSint.ZERO, i)
            );
            if (throwExceptionOnOOB) {
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


    protected void addSelectConstraintIfNeeded(SymbolicExecution se, Sarray sarray, Sint index, SubstitutedVar result) {
        // We will now add a constraint indicating to the solver that at position i a value can be found that previously
        // was not there. This only occurs if the array must be represented via constraints. This, in turn, only
        // is the case if symbolic indices have been used.
        if (sarray.__mulib__shouldBeRepresentedInSolver()) {
            if (result instanceof PartnerClass) {
                assert sarray instanceof Sarray.PartnerClassSarray;
                representPartnerClassObjectIfNeeded(se, (PartnerClass) result, sarray.__mulib__getId());
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

    protected abstract Sprimitive getValueToBeUsedForPartnerClassObjectConstraint(SubstitutedVar value);

    private void representPartnerClassObjectViaConstraintsIfNeeded(SymbolicExecution se, PartnerClass ihsv, Sint newIndex) {
        representPartnerClassObjectViaConstraintsIfNeeded(se, ihsv, newIndex instanceof Sym);
    }
    private void representPartnerClassObjectViaConstraintsIfNeeded(SymbolicExecution se, PartnerClass ihsv, boolean additionalConstraintToPrepare) {
        if (!ihsv.__mulib__shouldBeRepresentedInSolver() && additionalConstraintToPrepare) {
            ihsv.__mulib__prepareToRepresentSymbolically(se);
        }
        representPartnerClassObjectIfNeeded(se, ihsv, null);
    }

    @Override
    public SubstitutedVar getField(SymbolicExecution se, PartnerClass pco, String field) {
        assert pco.__mulib__isNull() == Sbool.ConcSbool.FALSE;
        assert pco.__mulib__isRepresentedInSolver();
        throw new NotYetImplementedException(); ///// TODO efficiently get value
    }

    @Override
    public SubstitutedVar putField(SymbolicExecution se, PartnerClass pco, String field, SubstitutedVar value) {
        assert pco.__mulib__isNull() == Sbool.ConcSbool.FALSE;
        assert pco.__mulib__isRepresentedInSolver();
        assert !(pco instanceof Sarray);
        if (!se.nextIsOnKnownPath()) {
            if (value instanceof PartnerClass) {
                if (!((PartnerClass) value).__mulib__shouldBeRepresentedInSolver()) {
                    //// TODO if pco has symbolic index, this value should also have a symbolic index, else not
                    throw new NotYetImplementedException();
                }
                representPartnerClassObjectIfNeeded(se, (PartnerClass) value, pco.__mulib__getId(), field);
            }
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
        return value;
    }

    @Override
    public void initializeLazyFields(SymbolicExecution se, PartnerClass pco) {
        assert pco.__mulib__defaultIsSymbolic();
        assert !pco.__mulib__isLazilyInitialized();
        if (pco.__mulib__isRepresentedInSolver()) {
            throw new NotYetImplementedException(); //// TODO
        }
        pco.__mulib__initializeLazyFields(se);
        pco.__mulib__setAsLazilyInitialized();
    }

    @Override
    public void representPartnerClassObjectIfNeeded(
            SymbolicExecution se,
            PartnerClass ihsr,
            Sint idOfContainingPartnerClassObject,
            String fieldName) {
        if (!ihsr.__mulib__shouldBeRepresentedInSolver() || ihsr.__mulib__isRepresentedInSolver()) {
            return;
        }
        assert !(ihsr instanceof Sarray) || ((Sarray) ihsr).getCachedIndices().stream().noneMatch(i -> i instanceof Sym)
                : "The Sarray should have already been represented in the constraint system";
        assert ihsr.__mulib__getId() != null;
        // Already set this so that we do not have any issues with circular dependencies
        ihsr.__mulib__setAsRepresentedInSolver();
        if (ihsr instanceof Sarray.PartnerClassSarray) {
            // Includes SarraySarrays
            for (Object objectEntry : ((Sarray) ihsr).getCachedElements()) {
                if (objectEntry == null) {
                    continue;
                }
                PartnerClass entry = (PartnerClass) objectEntry;
                if (!entry.__mulib__isRepresentedInSolver()) {
                    assert !entry.__mulib__shouldBeRepresentedInSolver();
                    entry.__mulib__prepareToRepresentSymbolically(se); // TODO For aliasing, check here
                    // Is a Sarray, i.e., we do not have a field here
                    representPartnerClassObjectIfNeeded(se, entry, ihsr.__mulib__getId(), null);
                }
                if (entry instanceof Sarray) {
                    assert entry.__mulib__cacheIsBlocked();
                    ((Sarray<?>) entry).clearCache();
                }
            }
        } else if (!(ihsr instanceof Sarray)) {
            // We must not initialize the fields, if they have not already been initialized
            if (!ihsr.__mulib__defaultIsSymbolic() || (ihsr.__mulib__defaultIsSymbolic() && ihsr.__mulib__isLazilyInitialized())) { //// TODO own method for this? should probably also be called in putField etc.
                Map<String, SubstitutedVar> fieldValues = ihsr.__mulib__getFieldNameToSubstitutedVar();
                for (Map.Entry<String, SubstitutedVar> entry : fieldValues.entrySet()) {
                    SubstitutedVar val = entry.getValue();
                    if (!(val instanceof PartnerClass)) {
                        continue;
                    }
                    if (val instanceof Sarray) {
                        // TODO Only possible for special case: Object[]
                        //  Should be implemented when implementing free objects
                        throw new NotYetImplementedException();
                    }
                    PartnerClass pc = (PartnerClass) val;
                    pc.__mulib__prepareToRepresentSymbolically(se); // TODO for aliasing, check here
                    representPartnerClassObjectIfNeeded(se, pc, ihsr.__mulib__getId(), entry.getKey());
                    //// TODO
                }
            }
        }

        // Represent array OR partnerclass object in constraint solver, if needed
        if (idOfContainingPartnerClassObject == null || ihsr.__mulib__getId() instanceof ConcSnumber) {
            // In this case the Sarray was not spawned from a select from a SarraySarray
            if (!se.nextIsOnKnownPath()) {
                if (ihsr instanceof Sarray) {
                    Sarray sarray = (Sarray) ihsr;
                    ArrayConstraint arrayInitializationConstraint = new ArrayInitializationConstraint(
                            (Sint) tryGetSymFromSnumber.apply(sarray.__mulib__getId()),
                            (Sint) tryGetSymFromSnumber.apply(sarray._getLengthWithoutCheckingForIsNull()),
                            tryGetSymFromSbool.apply(sarray.__mulib__isNull()),
                            sarray.getElementType(),
                            collectInitialArrayAccessConstraints(sarray, se),
                            sarray.__mulib__defaultIsSymbolic()
                    );
                    se.addNewPartnerClassObjectConstraint(arrayInitializationConstraint);
                } else {
                    assert ihsr.__mulib__getId() != null;
                    Map<String, Class<?>> fieldsToTypes = ihsr.__mulib__getFieldNameToType();
                    PartnerClassObjectInitializationConstraint c =
                            new PartnerClassObjectInitializationConstraint(
                                    ihsr.getClass(),
                                    (Sint) tryGetSymFromSnumber.apply(ihsr.__mulib__getId()),
                                    tryGetSymFromSbool.apply(ihsr.__mulib__isNull()),
                                    fieldsToTypes,
                                    collectInitialPartnerClassObjectFieldConstraints(ihsr, se),
                                    ihsr.__mulib__defaultIsSymbolic()
                            );
                    se.addNewPartnerClassObjectConstraint(c);
                }
            }
        } else {
            // In this case we must initialize the Sarray with the possibility of aliasing the elements in the sarray
            Sint nextNumberInitializedSymObject = se.concSint(se.getNextNumberInitializedSymObject());
            if (!se.nextIsOnKnownPath()) {
                if (ihsr instanceof Sarray) {
                    Sarray sarray = (Sarray) ihsr;
                    ArrayConstraint arrayInitializationConstraint = new ArrayInitializationConstraint(
                            (Sint) tryGetSymFromSnumber.apply(sarray.__mulib__getId()),
                            (Sint) tryGetSymFromSnumber.apply(sarray._getLengthWithoutCheckingForIsNull()),
                            tryGetSymFromSbool.apply(sarray.__mulib__isNull()),
                            // Id reserved for this Sarray, if needed
                            nextNumberInitializedSymObject,
                            (Sint) tryGetSymFromSnumber.apply(idOfContainingPartnerClassObject),
                            sarray.getElementType(),
                            collectInitialArrayAccessConstraints(sarray, se),
                            sarray.__mulib__defaultIsSymbolic()
                    );
                    se.addNewPartnerClassObjectConstraint(arrayInitializationConstraint);
                } else {
                    assert ihsr.__mulib__getId() != null;
                    Map<String, Class<?>> fieldsToTypes = ihsr.__mulib__getFieldNameToType();
                    PartnerClassObjectInitializationConstraint c =
                            new PartnerClassObjectInitializationConstraint(
                                    ihsr.getClass(),
                                    (Sint) tryGetSymFromSnumber.apply(ihsr.__mulib__getId()),
                                    tryGetSymFromSbool.apply(ihsr.__mulib__isNull()),
                                    nextNumberInitializedSymObject,
                                    (Sint) tryGetSymFromSnumber.apply(idOfContainingPartnerClassObject),
                                    fieldsToTypes,
                                    collectInitialPartnerClassObjectFieldConstraints(ihsr, se),
                                    ihsr.__mulib__defaultIsSymbolic()
                            );
                    se.addNewPartnerClassObjectConstraint(c);
                }
            }
        }
    }

    @Override
    public void representPartnerClassObjectIfNeeded(
            SymbolicExecution se,
            PartnerClass ihsr,
            // null if sarray does not belong to a SarraySarray or PartnerClass object that is to be represented:
            Sint idOfContainingSarraySarray) {
        representPartnerClassObjectIfNeeded(se, ihsr, idOfContainingSarraySarray, null);
    }

    @Override
    public PartnerClassObjectInformation getAvailableInformationOnPartnerClassObject(SymbolicExecution se, PartnerClass var, String field) {
        assert !(var instanceof Sarray);
        return se.getAvailableInformationOnPartnerClassObject((Sint) tryGetSymFromSnumber.apply(var.__mulib__getId()), field);
    }

    @Override
    public ArrayInformation getAvailableInformationOnArray(SymbolicExecution se, Sarray.PartnerClassSarray var) {
        return se.getAvailableInformationOnArray((Sint) tryGetSymFromSnumber.apply(var.__mulib__getId()));
    }

    private PartnerClassObjectFieldConstraint[] collectInitialPartnerClassObjectFieldConstraints(PartnerClass pc, SymbolicExecution se) {
        Map<String, SubstitutedVar> fieldsToValues = pc.__mulib__getFieldNameToSubstitutedVar();
        return fieldsToValues.entrySet().stream().map(e -> {
            Sprimitive value;
            if (e.getValue() instanceof Sprimitive) {
                value = (Sprimitive) e.getValue();
            } else {
                representPartnerClassObjectViaConstraintsIfNeeded(se, (PartnerClass) e.getValue(), true);
                value = ((PartnerClass) e.getValue()).__mulib__getId();
            }

            if (value instanceof Sbool) {
                value = tryGetSymFromSbool.apply((Sbool) value);
            } else if (value instanceof Snumber) {
                value = tryGetSymFromSnumber.apply((Snumber) value);
            } else if (value == null) {
                value = Sint.ConcSint.MINUS_ONE;
            } else {
                throw new NotYetImplementedException();
            }
            return new PartnerClassObjectFieldConstraint(
                    pc.__mulib__getId(),
                    e.getKey(),
                    value,
                    PartnerClassObjectFieldConstraint.Type.GETFIELD
            );
        }).toArray(PartnerClassObjectFieldConstraint[]::new);
    }

    private ArrayAccessConstraint[] collectInitialArrayAccessConstraints(Sarray sarray, SymbolicExecution se) {
        assert !se.nextIsOnKnownPath() && sarray.__mulib__shouldBeRepresentedInSolver();
        Set<Sint> cachedIndices = sarray.getCachedIndices();
        assert cachedIndices.stream().noneMatch(i -> i instanceof Sym) : "The Sarray should have already been represented in the constraint system";

        List<ArrayAccessConstraint> initialConstraints = new ArrayList<>();
        for (Sint i : cachedIndices) {
            SubstitutedVar value = sarray.getFromCacheForIndex(i);
            if (value instanceof Sarray<?>) {
                representPartnerClassObjectViaConstraintsIfNeeded(se, (Sarray) value, true);
            }
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

    protected abstract void _addIndexInBoundsConstraint(SymbolicExecution se, Sbool indexInBounds);

    private SubstitutedVar _selectWithSymbolicIndexes(SymbolicExecution se, Sarray sarray, Sint index) {
        SubstitutedVar result = sarray.getFromCacheForIndex(index);
        if (result != null) {
            return result;
        }

        representPartnerClassObjectViaConstraintsIfNeeded(se, sarray, index);
        checkIndexAccess(sarray, index, se);

        result = sarray.getNewValueForSelect(se);
        addSelectConstraintIfNeeded(se, sarray, index, result);
        sarray.setInCacheForIndexForSelect(index, result);
        // Needed for concolic execution
        additionalChecksAfterSelect(sarray, se);
        return result;
    }

    private SubstitutedVar _storeWithSymbolicIndexes(SymbolicExecution se, Sarray sarray, Sint index, SubstitutedVar value) {
        representPartnerClassObjectViaConstraintsIfNeeded(se, sarray, index);
        checkIndexAccess(sarray, index, se);
        Sarray.checkIfValueIsStorableForSarray(sarray, value);

        // Similarly to select, we will notify the solver, if needed, that the representation of the array has changed.
        if (sarray.__mulib__shouldBeRepresentedInSolver()) {
            // We must reset the cached elements, if a symbolic variable is present and store was used
            // This is because we can't be sure which index-element pair was overwritten
            sarray.clearCache();
            if (value instanceof Sarray<?>) {
                representPartnerClassObjectViaConstraintsIfNeeded(se, (Sarray) value, true);
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
        sarray.setInCacheForIndexForStore(index, value);
        return value;
    }

    protected void additionalChecksAfterSelect(Sarray sarray, SymbolicExecution se) {}

}
