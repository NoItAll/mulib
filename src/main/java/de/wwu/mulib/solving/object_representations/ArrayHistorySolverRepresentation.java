package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Snumber;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

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
        this.beforeStore =
                toCopy.beforeStore == null
                        ?
                        null
                        :
                        new ArrayHistorySolverRepresentation(toCopy.beforeStore);
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
        if (guard instanceof Sbool.ConcSbool && ((Sbool.ConcSbool) guard).isFalse()) {
            // We do not need to add anything to the history of array accesses, as this access is not valid
            return Sbool.ConcSbool.FALSE;
        }
        Constraint constraintForStoreOperation;
        Constraint indexEqualsToStoreIndex;
        Constraint resultForSelectOperations;
        // If we stored, we prioritize the stored index-value pair
        if (store != null) {
            assert beforeStore != null;
            indexEqualsToStoreIndex = Eq.newInstance(store.index, index);
            constraintForStoreOperation = elementsEqualConstraint(store.value, value);
            resultForSelectOperations = beforeStore.select(guard, index, value);
        } else {
            indexEqualsToStoreIndex = Sbool.ConcSbool.FALSE;
            constraintForStoreOperation = Sbool.ConcSbool.FALSE;
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
                } else {
                    resultForSelectOperations = elementsEqualConstraint(s.value, value);
                    break;
                }
            }
            Constraint valuesEqual = elementsEqualConstraint(s.value, value);
            resultForSelectOperations = And.newInstance(
                    implies(
                            s.guard,
                            implies(indexEqualsToSelectIndex, valuesEqual)
                    ),
                    resultForSelectOperations
            );
        }

        Constraint result =
                And.newInstance(
                        guard,
                        implies(indexEqualsToStoreIndex, constraintForStoreOperation),
                        implies(Not.newInstance(indexEqualsToStoreIndex), resultForSelectOperations)
                );
        selects.push(new ArrayAccessSolverRepresentation(guard, index, value));
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

    private static Constraint elementsEqualConstraint(SubstitutedVar s0, SubstitutedVar s1) {
        if (s0 instanceof Sbool && s1 instanceof Sbool) {
            return And.newInstance((Sbool) s0, (Sbool) s1);
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
            assert guard != null && index != null && value != null;//// TODO maybe value should be allowed to be null once the array-id-issue is fixed: this would allow us to represent null-arrays directly.
            this.guard = guard;
            this.index = index;
            this.value = value;
        }
    }
}
