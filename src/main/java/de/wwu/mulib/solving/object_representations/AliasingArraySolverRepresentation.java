package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.*;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;
import de.wwu.mulib.substitutions.primitives.SymNumericExpressionSprimitive;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an array with Sint --> Sprimitive. Is also used to symbolically represent an array of arrays
 * using the IDs of the respective Sarrays. Is enhanced with the capabilities to treat aliasing, i.e.,
 * this array could be a new array or a pre-existing array. It is expected that the ID of this array is symbolic
 * so that it can in fact represent another array.
 */
public class AliasingArraySolverRepresentation extends AbstractArraySolverRepresentation {
    private final Sint reservedId;
    private final Constraint metadataConstraintForPotentialIds;
    private final Set<IncrementalSolverState.ArrayRepresentation<ArraySolverRepresentation>> aliasedArrays;
    // Is Sarray containing this Sarray is completely initialized? If there is no containing Sarray, is false
    private final boolean containingSarrayIsCompletelyInitialized;

    public AliasingArraySolverRepresentation(
            final ArrayInitializationConstraint aic,
            final int level,
            final Set<Sint> potentialIds,
            final IncrementalSolverState.SymbolicArrayStates<ArraySolverRepresentation> symbolicArrayStates,
            boolean containingSarrayIsCompletelyInitialized) {
        super(
                aic.getArrayId(),
                aic.getArrayLength(),
                aic.getIsNull(),
                level,
                new ArrayHistorySolverRepresentation(aic.getInitialSelectConstraints()),
                aic.isCompletelyInitialized(),
                aic.getValueType()
        );
        this.containingSarrayIsCompletelyInitialized = containingSarrayIsCompletelyInitialized;
        this.reservedId = aic.getReservedId();
        assert arrayId instanceof SymNumericExpressionSprimitive;
        assert potentialIds != null && potentialIds.size() > 0 : "There always must be at least one potential aliasing candidate";
        this.aliasedArrays = new HashSet<>();
        Constraint metadataEqualsDependingOnId;
        if (containingSarrayIsCompletelyInitialized) {
            // If it is completely initialized, we do not need to use a reserved id
            metadataEqualsDependingOnId = Sbool.ConcSbool.FALSE;
        } else {
            @SuppressWarnings("redundant")
            Constraint reservedIdIsEqual = Eq.newInstance(arrayId, reservedId);
            metadataEqualsDependingOnId = reservedIdIsEqual;
        }
        for (Sint id : potentialIds) {
            IncrementalSolverState.ArrayRepresentation<ArraySolverRepresentation> ar =
                    symbolicArrayStates.getArraySolverRepresentationForId(id);
            assert ar != null : "All Sarrays for aliasingPotentialIds must be not null!";
            aliasedArrays.add(ar);
            ArraySolverRepresentation asr = ar.getNewestRepresentation();
            Constraint idsEqual = Eq.newInstance(id, arrayId);
            Constraint isNullsEqual = Or.newInstance(And.newInstance(isNull, asr.getIsNull()), And.newInstance(Not.newInstance(isNull), Not.newInstance(asr.getIsNull())));
            Constraint lengthsEqual = Eq.newInstance(length, asr.getLength());
            Constraint idEqualityImplies = And.newInstance(
                    idsEqual,
                    isNullsEqual,
                    lengthsEqual
            );
            metadataEqualsDependingOnId = Or.newInstance(metadataEqualsDependingOnId, idEqualityImplies);
        }
        this.metadataConstraintForPotentialIds = metadataEqualsDependingOnId;
    }

    private AliasingArraySolverRepresentation(
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
        super(arrayId, arrayLength, isNull, level, arrayHistorySolverRepresentation, isCompletelyInitialized, valueType);
        this.reservedId = reservedId;
        this.metadataConstraintForPotentialIds = metadataConstraintForPotentialIds;
        this.aliasedArrays = aliasedArrays;
        for (IncrementalSolverState.ArrayRepresentation<ArraySolverRepresentation> ar : aliasedArrays) {
            ArraySolverRepresentation asr = ar.getNewestRepresentation();
            if (asr.getLevel() != level) {
                ar.addNewRepresentation(asr.copyForNewLevel(level), level);
            }
        }
        this.containingSarrayIsCompletelyInitialized = containingSarrayIsCompletelyInitialized;
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
    public AliasingArraySolverRepresentation copyForNewLevel(int level) {
        return new AliasingArraySolverRepresentation(
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
    public Set<? extends Sprimitive> getPotentialValues() {
        Set<Sprimitive> result;
        if (!containingSarrayIsCompletelyInitialized) {
            result = currentRepresentation.getPotentialValues();
        } else {
            result = new HashSet<>();
        }

        for (IncrementalSolverState.ArrayRepresentation<ArraySolverRepresentation> ar : aliasedArrays) {
            ArraySolverRepresentation asr = ar.getNewestRepresentation();
            result.addAll(asr.getPotentialValues());
        }

        return result;
    }

    @Override
    public Set<? extends Sprimitive> getInitialConcreteAndStoredValues() {
        Set<Sprimitive> result;
        if (!containingSarrayIsCompletelyInitialized) {
            result = currentRepresentation.getInitialConcreteAndStoredValues();
        } else {
            result = new HashSet<>();
        }
        for (IncrementalSolverState.ArrayRepresentation<ArraySolverRepresentation> ar : aliasedArrays) {
            ArraySolverRepresentation asr = ar.getNewestRepresentation();
            result.addAll(asr.getInitialConcreteAndStoredValues());
        }

        return result;
    }

    public Constraint getMetadataConstraintForPotentialIds() {
        return metadataConstraintForPotentialIds;
    }
}
