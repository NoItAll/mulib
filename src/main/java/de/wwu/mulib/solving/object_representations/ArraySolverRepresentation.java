package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Snumber;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ArraySolverRepresentation {
    private final long arrayId;
    private final Deque<ArrayAccessSolverRepresentation> selects;
    private final ArrayAccessSolverRepresentation store;
    private final ArraySolverRepresentation beforeStore;

    public ArraySolverRepresentation(long arrayId) {
        this.arrayId = arrayId;
        this.selects = new ArrayDeque<>();
        this.store = null;
        this.beforeStore = null;
    }

    private ArraySolverRepresentation(ArraySolverRepresentation beforeStore) {
        this.arrayId = beforeStore.arrayId;
        this.selects = new ArrayDeque<>(beforeStore.selects);
        this.store = beforeStore.store;
        this.beforeStore =
                beforeStore.beforeStore == null
                        ?
                        null
                        :
                        new ArraySolverRepresentation(beforeStore.beforeStore);
    }

    private ArraySolverRepresentation(
            ArraySolverRepresentation beforeStore,
            ArrayAccessSolverRepresentation store) {
        assert store != null && beforeStore != null;
        this.arrayId = beforeStore.arrayId;
        this.selects = new ArrayDeque<>();
        this.store = store;
        // We do not directly reference the old object to keep it unmodified by the selects of other
        // alternative choice options
        this.beforeStore = new ArraySolverRepresentation(beforeStore);
    }

    public Constraint select(Sint index, SubstitutedVar value) {
        Constraint constraintForStoreOperation;
        Constraint indexEqualsToStoreIndex;
        Constraint resultForSelectOperations;
        // If we stored, we prioritize the stored index-value pair
        if (store != null) {
            assert beforeStore != null;
            indexEqualsToStoreIndex = Eq.newInstance(store.index, index);
            constraintForStoreOperation = elementsEqualConstraint(store.value, value);
            resultForSelectOperations = beforeStore.select(index, value);
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
                }
            }
            Constraint valuesEqual = elementsEqualConstraint(s.value, value);
            resultForSelectOperations = And.newInstance(List.of(implies(indexEqualsToSelectIndex, valuesEqual), resultForSelectOperations));
        }

        Constraint result =
                And.newInstance(
                        implies(indexEqualsToStoreIndex, constraintForStoreOperation),
                        implies(Not.newInstance(indexEqualsToStoreIndex), resultForSelectOperations)
                );
        selects.push(new ArrayAccessSolverRepresentation(index, value));
        return result;
    }

    public ArraySolverRepresentation store(Sint index, SubstitutedVar value) {
        return new ArraySolverRepresentation(
                this,
                new ArrayAccessSolverRepresentation(index, value)
        );
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
        private final Sint index;
        private final SubstitutedVar value;
        ArrayAccessSolverRepresentation(Sint index, SubstitutedVar value) {
            this.index = index;
            this.value = value;
        }
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof ArraySolverRepresentation
                && ((ArraySolverRepresentation) o).arrayId == this.arrayId;
    }

    @Override
    public int hashCode() {
        return (int) arrayId;
    }
}
