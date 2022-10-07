package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayAccessConstraint;
import de.wwu.mulib.constraints.ArrayConstraint;
import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.ArrayList;
import java.util.List;
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
        sarray.nullCheck();
        if (useEagerIndexesForFreeArrayPrimitiveElements) {
            return (Sprimitive) _selectWithEagerIndexes(se, sarray, index);
        } else {
            return (Sprimitive) _selectWithSymbolicIndexes(se, sarray, index);
        }
    }

    @Override
    public final Sprimitive store(SymbolicExecution se, Sarray sarray, Sint index, Sprimitive value) {
        sarray.nullCheck();
        if (useEagerIndexesForFreeArrayPrimitiveElements) {
            return (Sprimitive) _storeWithEagerIndexes(se, sarray, index, value);
        } else {
            return (Sprimitive) _storeWithSymbolicIndexes(se, sarray, index, value);
        }
    }

    @Override
    public final Sarray<?> select(SymbolicExecution se, Sarray.SarraySarray sarraySarray, Sint index) {
        sarraySarray.nullCheck();
        if (useEagerIndexesForFreeArrayObjectElements) {
            return (Sarray<?>) _selectWithEagerIndexes(se, sarraySarray, index);
        } else {
            return (Sarray<?>) _selectWithSymbolicIndexes(se, sarraySarray, index);
        }
    }

    @Override
    public final Sarray<?> store(SymbolicExecution se, Sarray.SarraySarray sarraySarray, Sint index, SubstitutedVar value) {
        sarraySarray.nullCheck();
        if (useEagerIndexesForFreeArrayObjectElements) {
            return (Sarray<?>) _storeWithEagerIndexes(se, sarraySarray, index, value);
        } else {
            return (Sarray<?>) _storeWithSymbolicIndexes(se, sarraySarray, index, value);
        }
    }

    @Override
    public final PartnerClass select(SymbolicExecution se, Sarray.PartnerClassSarray<?> partnerClassSarray, Sint index) {
        partnerClassSarray.nullCheck();
        if (useEagerIndexesForFreeArrayObjectElements) {
            return (PartnerClass) _selectWithEagerIndexes(se, partnerClassSarray, index);
        } else {
            return (PartnerClass) _selectWithSymbolicIndexes(se, partnerClassSarray, index);
        }
    }

    @Override
    public final PartnerClass store(SymbolicExecution se, Sarray.PartnerClassSarray<?> partnerClassSarray, Sint index, SubstitutedVar value) {
        partnerClassSarray.nullCheck();
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
        if (sarray.shouldBeRepresentedInSolver()) {
            if (result instanceof Sarray) {
                assert sarray instanceof Sarray.SarraySarray;
                representArrayIfNeeded(se, (Sarray) result, sarray.getId());
            }
            if (!se.nextIsOnKnownPath()) {
                result = getValueToBeRepresentedInSarray(result);
                ArrayConstraint selectConstraint =
                        new ArrayAccessConstraint(
                                (Sint) tryGetSymFromSnumber.apply(sarray.getId()),
                                (Sint) tryGetSymFromSnumber.apply(index),
                                result,
                                ArrayAccessConstraint.Type.SELECT
                        );
                se.addNewArrayConstraint(selectConstraint);
            }
        }
    }

    protected abstract SubstitutedVar getValueToBeRepresentedInSarray(SubstitutedVar value);

    private void representArrayViaConstraintsIfNeeded(SymbolicExecution se, Sarray sarray, Sint newIndex) {
        representArrayViaConstraintsIfNeeded(se, sarray, newIndex instanceof Sym);
    }
    private void representArrayViaConstraintsIfNeeded(SymbolicExecution se, Sarray sarray, boolean additionalConstraintToPrepare) {
        if (!sarray.shouldBeRepresentedInSolver() && additionalConstraintToPrepare) {
            sarray.prepareToRepresentSymbolically(se);
        }
        representArrayIfNeeded(se, sarray, null);
    }

    @Override
    public void representArrayIfNeeded(
            SymbolicExecution se,
            Sarray sarray,
            // null if sarray does not belong to a SarraySarray that is to be represented:
            Sint idOfContainingSarraySarray) {
        if (!sarray.shouldBeRepresentedInSolver() || sarray.isRepresentedInSolver()) {
            return;
        }
        Set<Sint> cachedIndices = sarray.getCachedIndices();
        assert cachedIndices.stream().noneMatch(i -> i instanceof Sym) : "The Sarray should have already been represented in the constraint system";

        sarray.checkIfSarrayIsKnownToHaveEveryIndexInitialized();

        if (sarray instanceof Sarray.SarraySarray) {
            Sarray.SarraySarray ss = (Sarray.SarraySarray) sarray;
            for (Sarray entry : ss.getCachedElements()) {
                if (entry == null) {
                    continue;
                }
                if (!entry.isRepresentedInSolver()) {
                    assert !entry.shouldBeRepresentedInSolver();
                    entry.prepareToRepresentSymbolically(se);
                    representArrayIfNeeded(se, entry, ss.getId());
                }
            }
        }

        ArrayConstraint arrayInitializationConstraint;
        // Represent array in constraint solver, if needed
        if (idOfContainingSarraySarray == null || sarray.getId() instanceof ConcSnumber) {
            // In this case the Sarray was not spawned from a select from a SarraySarray
            if (!sarray.isRepresentedInSolver()) {
                sarray.checkIfSarrayCanPotentiallyContainUnsetNonSymbolicDefaultAtInitialization();
                if (!se.nextIsOnKnownPath()) {
                    arrayInitializationConstraint = new ArrayInitializationConstraint(
                            (Sint) tryGetSymFromSnumber.apply(sarray.getId()),
                            (Sint) tryGetSymFromSnumber.apply(sarray._getLengthWithoutCheckingForIsNull()),
                            tryGetSymFromSbool.apply(sarray.isNull()),
                            sarray.getElementType(),
                            collectInitialArrayAccessConstraints(sarray, se),
                            sarray.defaultIsSymbolic()
                    );
                    se.addNewArrayConstraint(arrayInitializationConstraint);
                }
                sarray.setAsRepresentedInSolver();
            }
        } else {
            // In this case we must initialize the Sarray with the possibility of aliasing the elements in the sarray
            Sint nextNumberInitializedSymSarray = se.concSint(se.getNextNumberInitializedSymSarray());
            if (!sarray.isRepresentedInSolver()) {
                sarray.checkIfSarrayCanPotentiallyContainUnsetNonSymbolicDefaultAtInitialization();
                if (!se.nextIsOnKnownPath()) {
                    arrayInitializationConstraint = new ArrayInitializationConstraint(
                            (Sint) tryGetSymFromSnumber.apply(sarray.getId()),
                            (Sint) tryGetSymFromSnumber.apply(sarray._getLengthWithoutCheckingForIsNull()),
                            tryGetSymFromSbool.apply(sarray.isNull()),
                            // Id reserved for this Sarray, if needed
                            nextNumberInitializedSymSarray,
                            (Sint) tryGetSymFromSnumber.apply(idOfContainingSarraySarray),
                            sarray.getElementType(),
                            collectInitialArrayAccessConstraints(sarray, se),
                            sarray.defaultIsSymbolic()
                    );
                    se.addNewArrayConstraint(arrayInitializationConstraint);
                }
                sarray.setAsRepresentedInSolver();
            }
        }
    }

    protected ArrayAccessConstraint[] collectInitialArrayAccessConstraints(Sarray sarray, SymbolicExecution se) {
        assert !se.nextIsOnKnownPath() && sarray.shouldBeRepresentedInSolver() && !sarray.isRepresentedInSolver();
        Set<Sint> cachedIndices = sarray.getCachedIndices();
        assert cachedIndices.stream().noneMatch(i -> i instanceof Sym) : "The Sarray should have already been represented in the constraint system";

        List<ArrayAccessConstraint> initialConstraints = new ArrayList<>();
        for (Sint i : cachedIndices) {
            SubstitutedVar value = sarray.getFromCacheForIndex(i);
            if (value instanceof Sarray<?>) {
                representArrayViaConstraintsIfNeeded(se, (Sarray) value, true);
            }
            SubstitutedVar val = getValueToBeRepresentedInSarray(value);
            ArrayAccessConstraint ac = new ArrayAccessConstraint(
                    (Sint) tryGetSymFromSnumber.apply(sarray.getId()),
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

        representArrayViaConstraintsIfNeeded(se, sarray, index);
        checkIndexAccess(sarray, index, se);

        result = sarray.getNewValueForSelect(se);
        addSelectConstraintIfNeeded(se, sarray, index, result);
        sarray.setInCacheForIndexForSelect(index, result);
        additionalChecksAfterSelect(sarray, se);
        return result;
    }

    private SubstitutedVar _storeWithSymbolicIndexes(SymbolicExecution se, Sarray sarray, Sint index, SubstitutedVar value) {
        representArrayViaConstraintsIfNeeded(se, sarray, index);
        checkIndexAccess(sarray, index, se);
        Sarray.checkIfValueIsStorableForSarray(sarray, value);

        // Similarly to select, we will notify the solver, if needed, that the representation of the array has changed.
        if (sarray.shouldBeRepresentedInSolver()) {
            // We must reset the cached elements, if a symbolic variable is present and store was used
            // This is because we can't be sure which index-element pair was overwritten
            sarray.clearCache();
            if (value instanceof Sarray<?>) {
                representArrayViaConstraintsIfNeeded(se, (Sarray) value, true);
            }
            if (!se.nextIsOnKnownPath()) {
                SubstitutedVar inner = getValueToBeRepresentedInSarray(value);
                ArrayConstraint storeConstraint =
                        new ArrayAccessConstraint(
                                (Sint) tryGetSymFromSnumber.apply(sarray.getId()),
                                (Sint) tryGetSymFromSnumber.apply(index),
                                inner,
                                ArrayAccessConstraint.Type.STORE
                        );
                se.addNewArrayConstraint(storeConstraint);
            }
        }
        sarray.setInCacheForIndexForStore(index, value);
        return value;
    }

    protected void additionalChecksAfterSelect(Sarray sarray, SymbolicExecution se) {};

}
