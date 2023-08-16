package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.Set;

/**
 * Is used to symbolically represent an array of arrays or an array of partner class objects
 * using the IDs of the respective Sarrays.
 */
public class SimplePartnerClassArraySolverRepresentation extends PrimitiveValuedArraySolverRepresentation implements PartnerClassArraySolverRepresentation {

    /**
     * Constructs an array that is not aliasing and has reference-typed elements
     * @param config The configuration
     * @param aic The constraint initializing the representation
     * @param level The level
     */
    SimplePartnerClassArraySolverRepresentation(
            MulibConfig config,
            ArrayInitializationConstraint aic,
            int level) {
        super(config, aic, level);
        assert !Sprimitive.class.isAssignableFrom(aic.getValueType());
    }

    /**
     * The copy constructor
     * @param aasr To-copy
     * @param level The level to copy for
     */
    SimplePartnerClassArraySolverRepresentation(
            SimplePartnerClassArraySolverRepresentation aasr,
            int level) {
        super(aasr, level);
    }

    /**
     * Constructor for generating a representation lazily
     * @param config The configuration
     * @param id The identifier
     * @param length The length
     * @param isNull Whether the array is null
     * @param valueType The component type
     * @param defaultIsSymbolic Whether the default is symbolic
     * @param level The level
     * @param isCompletelyInitialized Whether the array is completely initialized
     * @param canPotentiallyContainCurrentlyUnrepresentedDefaults Whether there might potentially be unrepresented defaults
     */
    SimplePartnerClassArraySolverRepresentation(
            MulibConfig config,
            Sint id,
            Sint length,
            Sbool isNull,
            Class<?> valueType,
            boolean defaultIsSymbolic,
            int level,
            boolean isCompletelyInitialized,
            boolean canPotentiallyContainCurrentlyUnrepresentedDefaults) {
        super(config, id, length, isNull, valueType, defaultIsSymbolic, level, isCompletelyInitialized, canPotentiallyContainCurrentlyUnrepresentedDefaults);
    }

    @Override @SuppressWarnings("unchecked")
    public Set<Sint> getValuesKnownToPossiblyBeContainedInArray() {
        Set<Sint> result = (Set<Sint>) currentRepresentation.getValuesKnownToPossiblyBeContainedInArray(isCompletelyInitialized);
        return result;
    }

    @Override
    public SimplePartnerClassArraySolverRepresentation copyForNewLevel(int level) {
        return new SimplePartnerClassArraySolverRepresentation(
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
