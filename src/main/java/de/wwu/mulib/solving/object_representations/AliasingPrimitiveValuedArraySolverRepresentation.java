package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.And;
import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.Eq;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.Set;

/**
 * Represents an array with Sint --> Sprimitive. Is also used to symbolically represent an array of arrays
 * using the IDs of the respective Sarrays. Is enhanced with the capabilities to treat aliasing, i.e.,
 * this array could be a new array or a pre-existing array. It is expected that the ID of this array is symbolic
 * so that it can in fact represent another array.
 */
public class AliasingPrimitiveValuedArraySolverRepresentation extends AbstractAliasingArraySolverRepresentation {


    public AliasingPrimitiveValuedArraySolverRepresentation(
            final ArrayInitializationConstraint aic,
            final int level,
            final Set<Sint> potentialIds,
            final IncrementalSolverState.SymbolicArrayStates<ArraySolverRepresentation> symbolicArrayStates,
            boolean containingSarrayIsCompletelyInitialized) {
        super(aic, level, potentialIds, symbolicArrayStates, containingSarrayIsCompletelyInitialized);
        assert !aic.getValueType().isArray();
    }

    private AliasingPrimitiveValuedArraySolverRepresentation(
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
    public Constraint select(Constraint guard, Sint index, Sprimitive selectedValue) {
        if (guard instanceof Sbool.ConcSbool && ((Sbool.ConcSbool) guard).isFalse()) {
            return Sbool.ConcSbool.TRUE;
        }
        Constraint joinedSelectConstraint;
        if (containingSarrayIsCompletelyInitialized) {
            joinedSelectConstraint = Sbool.ConcSbool.TRUE;
        } else {
            // currentRepresentation here is the representation for this.reservedId
            @SuppressWarnings("redundant")
            Constraint ownConstraint =
                    this.currentRepresentation.select(
                            And.newInstance(guard, Eq.newInstance(arrayId, reservedId)),
                            index,
                            selectedValue,
                            isCompletelyInitialized
                    );
            joinedSelectConstraint = ownConstraint;
        }
        for (IncrementalSolverState.ArrayRepresentation<ArraySolverRepresentation> ar : aliasedArrays) {
            ArraySolverRepresentation asr = ar.getNewestRepresentation();
            Constraint partialSelectConstraint = asr.select(And.newInstance(guard, Eq.newInstance(arrayId, asr.getArrayId())), index, selectedValue);
            joinedSelectConstraint = And.newInstance(joinedSelectConstraint, partialSelectConstraint);
        }
        return joinedSelectConstraint;
    }

    @Override
    public void store(Constraint guard, Sint index, Sprimitive storedValue) {
        if (guard instanceof Sbool.ConcSbool && ((Sbool.ConcSbool) guard).isFalse()) {
            return;
        }

        for (IncrementalSolverState.ArrayRepresentation<ArraySolverRepresentation> ar : aliasedArrays) {
            ArraySolverRepresentation asr = ar.getNewestRepresentation();
            asr.store(And.newInstance(guard, Eq.newInstance(arrayId, asr.getArrayId())), index, storedValue);
        }
        if (!containingSarrayIsCompletelyInitialized) {
            // currentRepresentation here is the representation for this.reservedId
            this.currentRepresentation = this.currentRepresentation.store(And.newInstance(guard, Eq.newInstance(arrayId, reservedId)), index, storedValue);
        }
    }

    @Override
    public AliasingPrimitiveValuedArraySolverRepresentation copyForNewLevel(int level) {
        return new AliasingPrimitiveValuedArraySolverRepresentation(
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
}
