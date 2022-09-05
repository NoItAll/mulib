package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains the select for an array. A store yields a nested structure of ArrayHistorySolverRepresentations
 */
public class ArrayHistorySolverRepresentation {
    private final Deque<ArrayAccessSolverRepresentation> selects;
    private final ArrayAccessSolverRepresentation store;
    private final ArrayHistorySolverRepresentation beforeStore;

    public ArrayHistorySolverRepresentation() {
        this.selects = new ArrayDeque<>();
        this.store = null;
        this.beforeStore = null;
    }


    // Copy constructor, called to create a semantically equal version of ArraySolverRepresentation
    protected ArrayHistorySolverRepresentation(ArrayHistorySolverRepresentation toCopy) {
        this.selects = new ArrayDeque<>(toCopy.selects);
        this.store = toCopy.store;
        this.beforeStore = toCopy.beforeStore;
    }

    protected ArrayHistorySolverRepresentation(
            ArrayHistorySolverRepresentation beforeStore,
            ArrayAccessSolverRepresentation store) {
        assert store != null && beforeStore != null;
        this.selects = new ArrayDeque<>();
        this.store = store;
        // We do not directly reference the old object to keep it unmodified by the selects of other
        // alternative choice options
        this.beforeStore = beforeStore;
    }

    public ArrayHistorySolverRepresentation copy() {
        return new ArrayHistorySolverRepresentation(this);
    }

    public Constraint select(Constraint guard, Sint index, Sprimitive value) {
        return _select(guard, index, value, true);
    }

    private Constraint _select(Constraint guard, Sint index, Sprimitive value, boolean pushSelect) {
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
            resultForSelectOperations = beforeStore._select(guard, index, value, false);
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
            Constraint indexEqualsToSelectIndexImplication = implies(And.newInstance(s.guard, indexEqualsToSelectIndex), valuesEqual);
            resultForSelectOperations = And.newInstance(
                    indexEqualsToSelectIndexImplication,
                    resultForSelectOperations
            );
        }

        Constraint indexDoesNotEqualToStoreImplication = implies(Not.newInstance(indexEqualsToStoreIndexWithGuard), resultForSelectOperations);
        Constraint bothCasesImplications = And.newInstance(indexEqualsToStoreImplication, indexDoesNotEqualToStoreImplication);

        Constraint result = implies(guard, bothCasesImplications);
        if (pushSelect) {
            selects.push(new ArrayAccessSolverRepresentation(guard, index, value));
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

    public Set<Sprimitive> getPotentialValues() {
        Set<Sprimitive> result = new HashSet<>();
        if (beforeStore != null) {
            assert store != null;
            result.addAll(beforeStore.getPotentialValues());
            result.add(store.value);
        }
        for (ArrayAccessSolverRepresentation aasr : this.selects) {
            result.add(aasr.value);
        }
        return result;
    }

    public Set<Sprimitive> getInitialConcreteAndStoredValues() {
        Set<Sprimitive> result = new HashSet<>();
        ArrayHistorySolverRepresentation current = this;
        while (current.store != null) {
            result.add(store.value);
            assert current.beforeStore != null;
            current = beforeStore;
        }
        for (ArrayAccessSolverRepresentation aasr : current.selects) {
            // Get the selects from the first representation which are not symbolic
            if (aasr.index instanceof ConcSnumber && Sbool.ConcSbool.TRUE.equals(aasr.guard)) {
                result.add(aasr.value);
            }
        }
        return result;
    }

    private static Constraint elementsEqualConstraint(SubstitutedVar s0, SubstitutedVar s1) {
        if (s0 instanceof Sbool && s1 instanceof Sbool) {
            return Or.newInstance(
                    And.newInstance((Sbool) s0, (Sbool) s1),
                    And.newInstance(Not.newInstance((Sbool) s0), Not.newInstance((Sbool) s1))
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
