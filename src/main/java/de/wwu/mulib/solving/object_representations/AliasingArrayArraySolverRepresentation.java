package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.HashSet;
import java.util.Set;

public class AliasingArrayArraySolverRepresentation extends AbstractAliasingArraySolverRepresentation implements IArrayArraySolverRepresentation {

    public AliasingArrayArraySolverRepresentation(
            ArrayInitializationConstraint aic,
            int level,
            Set<Sint> potentialIds,
            IncrementalSolverState.SymbolicArrayStates<ArraySolverRepresentation> symbolicArrayStates,
            boolean containingSarrayIsCompletelyInitialized) {
        super(aic, level, potentialIds, symbolicArrayStates, containingSarrayIsCompletelyInitialized);
        assert aic.getValueType().isArray();
    }



    private AliasingArrayArraySolverRepresentation(
            Sint arrayId,
            Sint reservedId,
            Sint arrayLength,
            Sbool isNull,
            boolean isCompletelyInitialized,
            int level,
            Constraint metadataConstraintForPotentialIds,
            Set<IncrementalSolverState.ArrayRepresentation<ArraySolverRepresentation>> aliasedArrays,
            ArrayHistorySolverRepresentation arrayHistorySolverRepresentation,
            boolean containingSarrayIsCompletelyInitialized,
            Class<?> valueType) {
        super(
                arrayId,
                reservedId,
                arrayLength,
                isNull,
                isCompletelyInitialized,
                level,
                metadataConstraintForPotentialIds,
                aliasedArrays,
                arrayHistorySolverRepresentation,
                containingSarrayIsCompletelyInitialized,
                valueType
        );
    }

    @Override
    public Set<Sint> getPotentialValues() {
        Set<Sint> result;
        if (!containingSarrayIsCompletelyInitialized) {
            result = (Set<Sint>) currentRepresentation.getPotentialValues();
        } else {
            result = new HashSet<>();
        }

        for (IncrementalSolverState.ArrayRepresentation<ArraySolverRepresentation> ar : aliasedArrays) {
            ArraySolverRepresentation asr = ar.getNewestRepresentation();
            assert asr instanceof IArrayArraySolverRepresentation;
            result.addAll(((IArrayArraySolverRepresentation) asr).getPotentialValues());
        }

        return result;
    }

    @Override
    public Set<Sint> getInitialConcreteAndStoredValues() {
        Set<Sint> result;
        if (!containingSarrayIsCompletelyInitialized) {
            result = (Set<Sint>) currentRepresentation.getInitialConcreteAndStoredValues();
        } else {
            result = new HashSet<>();
        }
        for (IncrementalSolverState.ArrayRepresentation<ArraySolverRepresentation> ar : aliasedArrays) {
            ArraySolverRepresentation asr = ar.getNewestRepresentation();
            assert asr instanceof IArrayArraySolverRepresentation;
            result.addAll(((IArrayArraySolverRepresentation) asr).getInitialConcreteAndStoredValues());
        }

        return result;
    }

    @Override
    public IArrayArraySolverRepresentation copyForNewLevel(int level) {
        return new AliasingArrayArraySolverRepresentation(
                arrayId,
                reservedId,
                length,
                isNull,
                isCompletelyInitialized,
                level,
                metadataConstraintForPotentialIds,
                aliasedArrays,
                currentRepresentation.copy(),
                containingSarrayIsCompletelyInitialized,
                valueType
        );
    }

    @Override
    public Constraint select(Constraint guard, Sint index, Sprimitive selectedValue) {
        throw new NotYetImplementedException(); //// TODO
    }

    @Override
    public void store(Constraint guard, Sint index, Sprimitive storedValue) {
        throw new NotYetImplementedException(); //// TODO
    }
}
