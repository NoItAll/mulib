package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.HashSet;
import java.util.Set;

public class AliasingArrayArraySolverRepresentation extends AliasingPrimitiveValuedArraySolverRepresentation implements IArrayArraySolverRepresentation {

    public AliasingArrayArraySolverRepresentation(
            MulibConfig config,
            ArrayInitializationConstraint aic,
            int level,
            Set<Sint> potentialIds,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> symbolicArrayStates,
            boolean containingSarrayIsCompletelyInitialized) {
        super(config, aic, level, potentialIds, symbolicArrayStates, containingSarrayIsCompletelyInitialized);
        assert aic.getValueType().isArray();
    }

    private AliasingArrayArraySolverRepresentation(
            AliasingArrayArraySolverRepresentation aaasr,
            int level) {
        super(aaasr, level);
    }

    @Override @SuppressWarnings("unchecked")
    public Set<Sint> getValuesKnownToPossiblyBeContainedInArray() {
        Set<Sint> result;
        if (!containingSarrayIsCompletelyInitialized) {
            result = (Set<Sint>) currentRepresentation.getValuesKnownToPossiblyBeContainedInArray(isCompletelyInitialized);
        } else {
            result = new HashSet<>();
        }

        for (IncrementalSolverState.PartnerClassObjectRepresentation<ArraySolverRepresentation> ar : aliasedArrays) {
            ArraySolverRepresentation asr = ar.getNewestRepresentation();
            assert asr instanceof IArrayArraySolverRepresentation;
            result.addAll(((IArrayArraySolverRepresentation) asr).getValuesKnownToPossiblyBeContainedInArray());
        }

        return result;
    }

    @Override
    public AliasingArrayArraySolverRepresentation copyForNewLevel(int level) {
        return new AliasingArrayArraySolverRepresentation(
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
