package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Contains the select for an array. A store yields a nested structure of ArrayHistorySolverRepresentations
 */
public class ArrayHistorySolverRepresentation {
    private final List<ArrayAccessSolverRepresentation> selects;
    private final ArrayAccessSolverRepresentation store;
    private final ArrayHistorySolverRepresentation beforeStore;
    private final SubstitutedVar defaultValue;

    public ArrayHistorySolverRepresentation(
            ArrayAccessConstraint[] initialSelects,
            Class<?> valueType) {
        SubstitutedVar defaultValue;
         if (Sbool.class.isAssignableFrom(valueType)) {
            defaultValue = Sbool.ConcSbool.FALSE;
        } else if (Sbyte.class.isAssignableFrom(valueType)) {
            defaultValue = Sbyte.ConcSbyte.ZERO;
        } else if (Sshort.class.isAssignableFrom(valueType)) {
            defaultValue = Sshort.ConcSshort.ZERO;
        } else if (Sint.class.isAssignableFrom(valueType)) {
            defaultValue = Sint.ConcSint.ZERO;
        } else if (Slong.class.isAssignableFrom(valueType)) {
            defaultValue = Slong.ConcSlong.ZERO;
        } else if (Sdouble.class.isAssignableFrom(valueType)) {
            defaultValue = Sdouble.ConcSdouble.ZERO;
        } else if (Sfloat.class.isAssignableFrom(valueType)) {
            defaultValue = Sfloat.ConcSfloat.ZERO;
        } else if (valueType.isArray() || PartnerClass.class.isAssignableFrom(valueType)) {
             defaultValue = Sint.ConcSint.MINUS_ONE;
         } else {
            throw new NotYetImplementedException();
        }
        this.defaultValue = defaultValue;
        this.selects = new ArrayList<>();
        for (ArrayAccessConstraint ac : initialSelects) {
            Sprimitive value = ac.getValue();
            if (value == null) {
                // TODO Perhaps find more elegant, less hard-wired way
                assert valueType.isArray();
                value = Sint.ConcSint.MINUS_ONE;
            }
            selects.add(new ArrayAccessSolverRepresentation(
                    Sbool.ConcSbool.TRUE,
                    ac.getIndex(),
                    value
            ));
        }
        this.store = null;
        this.beforeStore = null;
    }


    // Copy constructor, called to create a semantically equal version of ArraySolverRepresentation
    protected ArrayHistorySolverRepresentation(ArrayHistorySolverRepresentation toCopy) {
        this.selects = new ArrayList<>(toCopy.selects);
        this.store = toCopy.store;
        this.beforeStore = toCopy.beforeStore;
        this.defaultValue = toCopy.defaultValue;
    }

    protected ArrayHistorySolverRepresentation(
            ArrayHistorySolverRepresentation beforeStore,
            ArrayAccessSolverRepresentation store) {
        assert store != null && beforeStore != null;
        this.selects = new ArrayList<>();
        this.store = store;
        // We do not directly reference the old object to keep it unmodified by the selects of other
        // alternative choice options
        this.beforeStore = beforeStore;
        this.defaultValue = beforeStore.defaultValue;
    }

    public ArrayHistorySolverRepresentation copy() {
        return new ArrayHistorySolverRepresentation(this);
    }

    public Constraint select(
            Constraint guard,
            Sint index,
            Sprimitive value,
            boolean arrayIsCompletelyInitialized,
            boolean defaultValueForUnknownsShouldBeEnforced) {
        return _select(
                guard,
                index,
                value,
                !arrayIsCompletelyInitialized,
                // We do not have to enforce that, if index is unseen, value is a default value, if
                // there are not unseen indices
                defaultValueForUnknownsShouldBeEnforced
        );
    }

    private Constraint _select(
            Constraint guard,
            Sint index,
            Sprimitive value,
            boolean pushSelect,
            boolean defaultValueForUnknownsShouldBeEnforced) {
        if (guard instanceof Sbool.ConcSbool && ((Sbool.ConcSbool) guard).isFalse()) {
            // We do not need to add anything to the history of array accesses, as this access is not valid
            return Sbool.ConcSbool.TRUE;
        }
        Constraint indexEqualsToStoreIndexWithGuard;
        Constraint indexEqualsToStoreImplication;
        Constraint resultForSelectOperations;
        // If we stored, we prioritize the stored index-value pair
        if (store != null) {
            assert beforeStore != null;
            indexEqualsToStoreIndexWithGuard = And.newInstance(store.guard, Eq.newInstance(store.index, index));
            Constraint constraintForStoreOperation = elementsEqualConstraint(store.value, value);
            indexEqualsToStoreImplication = implies(indexEqualsToStoreIndexWithGuard, constraintForStoreOperation);
            resultForSelectOperations = beforeStore._select(guard, index, value, false, false);
        } else {
            indexEqualsToStoreIndexWithGuard = Sbool.ConcSbool.FALSE;
            indexEqualsToStoreImplication = Sbool.ConcSbool.TRUE;
            resultForSelectOperations = Sbool.ConcSbool.TRUE;
        }


        // If it is not clear that the value must stem from the store operation, we check all previous selected values
        for (ArrayAccessSolverRepresentation s : selects) {
            Constraint indexEqualsToSelectIndex = Eq.newInstance(s.index, index);
            if (indexEqualsToSelectIndex instanceof Sbool.ConcSbool) {
                // We can cut this short:
                boolean doEqual = ((Sbool.ConcSbool) indexEqualsToSelectIndex).isTrue();
                if (!doEqual) {
                    // We can simply skip this index
                    continue;
                } else if (s.guard instanceof Sbool.ConcSbool && ((Sbool.ConcSbool) s.guard).isTrue()) {
                    resultForSelectOperations = elementsEqualConstraint(s.value, value);
                    break;
                }
            }
            Constraint valuesEqual = elementsEqualConstraint(s.value, value);
            Constraint indexEqualsToSelectIndexWithGuard = And.newInstance(s.guard, indexEqualsToSelectIndex);
            Constraint indexEqualsToSelectIndexImplication =
                    implies(indexEqualsToSelectIndexWithGuard, valuesEqual);
            resultForSelectOperations = And.newInstance(
                    indexEqualsToSelectIndexImplication,
                    resultForSelectOperations
            );
        }

        Constraint indexDoesNotEqualToStoreImplication =
                implies(Not.newInstance(indexEqualsToStoreIndexWithGuard), resultForSelectOperations);
        Constraint bothCasesImplications =
                And.newInstance(indexEqualsToStoreImplication, indexDoesNotEqualToStoreImplication);

        if (defaultValueForUnknownsShouldBeEnforced) {
            // If we have to enforce a default value, for instance 0 for int arrays or -1 for array-arrays, for unknown values, we
            // have to gather all guarded index-equals-constraints to use later
            Constraint indexEqualsToAnyPreviousIndexWithGuard =
                    getFromHistory(h -> And.newInstance(h.guard, Eq.newInstance(index, h.index)))
                            .stream()
                            .reduce(Sbool.ConcSbool.FALSE, Or::newInstance);

            // If the default value is used for unset values, we must check if it is a currently unrepresented index and
            // in this case, imply that the default value is used. E.g. 0 for int-arrays
            // For objects, -1 is used to signal null
            // If the defaultValue is not enforced for unknown index-value pairs, defaultValueForUnknownPossible is
            // false and 'value' is unrestricted
            bothCasesImplications =
                    And.newInstance(
                            // bothCasesImplications must hold
                            bothCasesImplications,
                            // Furthermore, it must hold that if neither the index-equals constraints for the store or selects
                            // hold, the value must equal to the default value
                            implies(
                                    Not.newInstance(indexEqualsToAnyPreviousIndexWithGuard),
                                    elementsEqualConstraint(value, defaultValue)
                            )
                    );
        }

        Constraint result = implies(guard, bothCasesImplications);
        if (pushSelect) {
            selects.add(new ArrayAccessSolverRepresentation(guard, index, value));
        }
        return result;
    }

    public ArrayHistorySolverRepresentation store(Constraint guard, Sint index, Sprimitive value) {
        if (guard instanceof Sbool.ConcSbool && ((Sbool.ConcSbool) guard).isFalse()) {
            return this;
        }
        return new ArrayHistorySolverRepresentation(
                this,
                new ArrayAccessSolverRepresentation(guard, index, value)
        );
    }

    public Set<? extends Sprimitive> getValuesKnownToPossiblyBeContainedInArray(boolean arrayHasFixedLength) {
        // In the case where the array has a fixed length, the array is fully initialized directly. We only have
        // to regard the initially selected values as well as the stored values
        if (arrayHasFixedLength) {
            return getInitialConcreteAndStoredFromHistory(h -> h.value);
        } else {
            return getFromHistory(h -> h.value);
        }
    }

    public Set<Sint> getIndicesKnownToPossiblyBeContainedInArray(boolean arrayHasFixedLength) {
        // In the case where the array has a fixed length, the array is fully initialized directly. And all indices
        // are already given in the first layer of the array
        if (arrayHasFixedLength) {
            return getInitialConcreteAndStoredFromHistory(h -> h.index);
        } else {
            return getFromHistory(h -> h.index);
        }
    }

    private <T> Set<T> getFromHistory(Function<ArrayAccessSolverRepresentation, T> get) {
        Set<T> result = new HashSet<>();
        if (beforeStore != null) {
            assert store != null;
            result.addAll(beforeStore.getFromHistory(get));
            result.add(get.apply(store));
        }
        for (ArrayAccessSolverRepresentation aasr : this.selects) {
            result.add(get.apply(aasr));
        }
        return result;
    }

    private <T> Set<T> getInitialConcreteAndStoredFromHistory(Function<ArrayAccessSolverRepresentation, T> get) {
        Set<T> result = new HashSet<>();
        ArrayHistorySolverRepresentation current = this;
        while (current.store != null) {
            result.add(get.apply(current.store));
            assert current.beforeStore != null;
            current = current.beforeStore;
        }
        for (ArrayAccessSolverRepresentation aasr : current.selects) {
            // Get the selects from the first representation which are not symbolic
            if (aasr.index instanceof ConcSnumber && Sbool.ConcSbool.TRUE.equals(aasr.guard)) {
                result.add(get.apply(aasr));
            }
        }
        return result;
    }

    private static Constraint elementsEqualConstraint(SubstitutedVar s0, SubstitutedVar s1) {
        if (s0 instanceof Sbool && s1 instanceof Sbool) {
            return Or.newInstance(
                    And.newInstance((Sbool) s0, (Sbool) s1),
                    Not.newInstance(Or.newInstance((Sbool) s0, (Sbool) s1))
            );
        } else if (s0 instanceof Snumber) {
            return Eq.newInstance((Snumber) s0, (Snumber) s1);
        } else {
            throw new NotYetImplementedException();
        }
    }

    private static Constraint implies(Constraint c0, Constraint c1) {
        return Or.newInstance(Not.newInstance(c0), c1);
    }

    private static class ArrayAccessSolverRepresentation {
        // The guard will typically be if the arrayId belonging to this ArrayHistorySolverRepresentation
        // is equal to some other symbolic ID
        private final Constraint guard;
        private final Sint index;
        private final Sprimitive value;
        ArrayAccessSolverRepresentation(Constraint guard, Sint index, Sprimitive value) {
            assert guard != null && index != null && value != null;
            this.guard = guard;
            this.index = index;
            this.value = value;
        }
    }
}
