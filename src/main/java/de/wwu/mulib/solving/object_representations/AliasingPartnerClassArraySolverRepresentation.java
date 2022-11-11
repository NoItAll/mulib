package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.HashSet;
import java.util.Set;

public class AliasingPartnerClassArraySolverRepresentation extends AliasingPrimitiveValuedArraySolverRepresentation implements PartnerClassArraySolverRepresentation {

    private final IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps;
    private final IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr;

    public AliasingPartnerClassArraySolverRepresentation(
            MulibConfig config,
            ArrayInitializationConstraint aic,
            int level,
            Set<Sint> potentialIds,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr,
            boolean containingSarrayIsCompletelyInitialized) {
        super(config, aic, level, potentialIds, asr, containingSarrayIsCompletelyInitialized);
        this.sps = sps;
        this.asr = asr;
        assert aic.getValueType().isArray() || PartnerClass.class.isAssignableFrom(aic.getValueType());
    }

    private AliasingPartnerClassArraySolverRepresentation(
            AliasingPartnerClassArraySolverRepresentation aaasr,
            int level) {
        super(aaasr, level);
        this.asr = aaasr.asr;
        this.sps = aaasr.sps;
    }

    /**
     * Constructor for generating lazily
     */
    protected AliasingPartnerClassArraySolverRepresentation(
            MulibConfig config,
            Sint id,
            Sint length,
            Sbool isNull,
            Class<?> valueType,
            boolean defaultIsSymbolic,
            int level,
            Sint reservedId,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr,
            boolean cannotBeNewInstance,
            boolean isCompletelyInitialized,
            boolean canPotentiallyContainCurrentlyUnrepresentedDefaults,
            Set<Sint> potentialIds) {
        super(config, id, length, isNull, valueType, defaultIsSymbolic, level, reservedId, asr, cannotBeNewInstance,
                isCompletelyInitialized, canPotentiallyContainCurrentlyUnrepresentedDefaults, potentialIds);
        this.sps = sps;
        this.asr = asr;
    }

    @Override @SuppressWarnings("unchecked")
    public Set<Sint> getValuesKnownToPossiblyBeContainedInArray() {
        Set<Sint> result;
        if (!cannotBeNewInstance) {
            result = (Set<Sint>) currentRepresentation.getValuesKnownToPossiblyBeContainedInArray(isCompletelyInitialized);
        } else {
            result = new HashSet<>();
        }

        for (IncrementalSolverState.PartnerClassObjectRepresentation<ArraySolverRepresentation> ar : aliasedArrays) {
            ArraySolverRepresentation asr = getAliasLevelSafe(ar);
            assert asr instanceof PartnerClassArraySolverRepresentation && asr.getLevel() >= level;
            result.addAll(((PartnerClassArraySolverRepresentation) asr).getValuesKnownToPossiblyBeContainedInArray());
        }

        return result;
    }

    @Override
    public AliasingPartnerClassArraySolverRepresentation copyForNewLevel(int level) {
        return new AliasingPartnerClassArraySolverRepresentation(
                this, level
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
