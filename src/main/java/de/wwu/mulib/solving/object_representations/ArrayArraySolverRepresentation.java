package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.Set;

/**
 * Is used to symbolically represent an array of arrays
 * using the IDs of the respective Sarrays.
 */
public class ArrayArraySolverRepresentation extends PrimitiveValuedArraySolverRepresentation implements IArrayArraySolverRepresentation {

    public ArrayArraySolverRepresentation(
            ArrayInitializationConstraint aic,
            int level) {
        super(aic, level);
        assert aic.getValueType().isArray();
    }

    private ArrayArraySolverRepresentation(
            ArrayArraySolverRepresentation aasr,
            int level) {
        super(aasr, level);
    }

    @Override @SuppressWarnings("unchecked")
    public Set<Sint> getValuesKnownToPossiblyBeContainedInArray() {
        return (Set<Sint>) currentRepresentation.getValuesKnownToPossiblyBeContainedInArray(isCompletelyInitialized);
    }

    @Override
    public ArrayArraySolverRepresentation copyForNewLevel(int level) {
        return new ArrayArraySolverRepresentation(
                this,
                level
        );
    }

    @Override
    protected Constraint _select(Constraint guard, Sint index, Sprimitive selectedValue) {
        assert selectedValue != Sint.ConcSint.MINUS_ONE;
        return super._select(guard, index, selectedValue);
    }

    @Override
    protected void _store(Constraint guard, Sint index, Sprimitive storedValue) {
        if (storedValue == null) {
            storedValue = Sint.ConcSint.MINUS_ONE;
        }
        super._store(guard, index, storedValue);
    }
}
