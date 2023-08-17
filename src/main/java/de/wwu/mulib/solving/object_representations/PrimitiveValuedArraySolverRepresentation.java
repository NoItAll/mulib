package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

/**
 * Represents an array with Sint --> Sprimitive.
 */
public class PrimitiveValuedArraySolverRepresentation extends AbstractArraySolverRepresentation {

    /**
     * Constructs an array that is neither aliasing nor has reference-typed elements
     * @param config The configuration
     * @param aic The constraint initializing the representation
     * @param level The level
     */
    PrimitiveValuedArraySolverRepresentation(MulibConfig config, ArrayInitializationConstraint aic, int level) {
        super(config, aic, level);
    }

    /**
     * Copy constructor
     * @param pvasr The representation to copy
     * @param level The level to copy for
     */
    protected PrimitiveValuedArraySolverRepresentation(
            PrimitiveValuedArraySolverRepresentation pvasr,
            int level) {
        super(pvasr, level);
    }

    /**
     * Constructor for generating lazily
     * @param config The configuration
     * @param id The identifier
     * @param length The length
     * @param isNull Whether the array can be null
     * @param valueType The component type
     * @param defaultIsSymbolic Whether the default is symbolic
     * @param level The level
     * @param isCompletelyInitialized Whether the array is completely initialized
     * @param canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaults Whether there may be unrepresented non-symbolic defaults
     */
    protected PrimitiveValuedArraySolverRepresentation(
            MulibConfig config,
            Sint id,
            Sint length,
            Sbool isNull,
            Class<?> valueType,
            boolean defaultIsSymbolic,
            int level,
            boolean isCompletelyInitialized,
            boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaults) {
        super(config, id, length, isNull, valueType, defaultIsSymbolic, isCompletelyInitialized, canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaults, level);
    }

    @Override
    protected Constraint _select(Constraint guard, Sint index, Sprimitive selectedValue) {
        // The metadata constraint of the selected sarray will already validly restrict the id values
        return currentRepresentation.select(
                guard,
                index,
                selectedValue,
                // If the array is completely initialized, we do not have to push this index-value combination since
                // it is already represented
                isCompletelyInitialized,
                config.FREE_INIT_ALIASING_FOR_FREE_OBJECTS,
                valueType,
                // We only must enforce the default for unknown values if !defaultIsSymbolic
                // Otherwise, we would enforce that defaultIsSymbolic-objects are always initialized to null
                !defaultIsSymbolic && canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault
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

    @Override
    public String toString() {
        return String.format("%s[%s]{length=%s, isNull=%s, currentRepresentation=%s}", this instanceof SimplePartnerClassArraySolverRepresentation ? "PCArrayRep" : "PrimArrayRep", arrayId, length, isNull, currentRepresentation);
    }
}
