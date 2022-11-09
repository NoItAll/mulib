package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.Set;

/**
 * Is used to symbolically represent an array of arrays
 * using the IDs of the respective Sarrays.
 */
public class SimplePartnerClassArraySolverRepresentation extends PrimitiveValuedArraySolverRepresentation implements PartnerClassArraySolverRepresentation {
    private final IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps;
    private final IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr;

    public SimplePartnerClassArraySolverRepresentation(
            MulibConfig config,
            ArrayInitializationConstraint aic,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr,
            int level) {
        super(config, aic, level);
        assert !Sprimitive.class.isAssignableFrom(aic.getValueType());
        this.sps = sps;
        this.asr = asr;
    }

    private SimplePartnerClassArraySolverRepresentation(
            SimplePartnerClassArraySolverRepresentation aasr,
            int level) {
        super(aasr, level);
        this.sps = aasr.sps;
        this.asr = aasr.asr;
    }

    /**
     * Constructor for generating lazily
     */
    protected SimplePartnerClassArraySolverRepresentation(
            MulibConfig config,
            Sint id,
            Sint length,
            Sbool isNull,
            Class<?> valueType,
            boolean defaultIsSymbolic,
            int level,
            boolean isCompletelyInitialized,
            boolean canPotentiallyContainCurrentlyUnrepresenteDefaults,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr) {
        super(config, id, length, isNull, valueType, defaultIsSymbolic, level, isCompletelyInitialized, canPotentiallyContainCurrentlyUnrepresenteDefaults);
        this.asr = asr;
        this.sps = sps;
    }

    @Override @SuppressWarnings("unchecked")
    public Set<Sint> getValuesKnownToPossiblyBeContainedInArray() {
        return (Set<Sint>) currentRepresentation.getValuesKnownToPossiblyBeContainedInArray(isCompletelyInitialized);
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
