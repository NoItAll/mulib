package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

/**
 * Represents an array with Sint --> Sprimitive.
 */
public class PrimitiveValuedArraySolverRepresentation extends AbstractArraySolverRepresentation {

    public PrimitiveValuedArraySolverRepresentation(MulibConfig config, ArrayInitializationConstraint aic, int level) {
        super(config, aic, level);
    }

    protected PrimitiveValuedArraySolverRepresentation(
            PrimitiveValuedArraySolverRepresentation pvasr,
            int level) {
        super(pvasr, level);
    }

    @Override
    protected Constraint _select(Constraint guard, Sint index, Sprimitive selectedValue) {
        return currentRepresentation.select(
                guard,
                index,
                selectedValue,
                // If the array is completely initialized, we do not have to push this index-value combination since
                // it is already represented
                isCompletelyInitialized,
                this instanceof IArrayArraySolverRepresentation && defaultIsSymbolic ?
                        false // The metadata constraint of the selected sarray will already validly restrict the id values
                        :
                        canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault
        );
    }

    @Override
    protected void _store(Constraint guard, Sint index, Sprimitive storedValue) {
        this.currentRepresentation = currentRepresentation.store(guard, index, storedValue);
    }

    @Override
    public PrimitiveValuedArraySolverRepresentation copyForNewLevel(int level) {
        return new PrimitiveValuedArraySolverRepresentation(
                this, level
        );
    }
}
