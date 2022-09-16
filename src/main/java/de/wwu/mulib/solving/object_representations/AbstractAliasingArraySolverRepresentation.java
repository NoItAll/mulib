package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.*;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.SymNumericExpressionSprimitive;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractAliasingArraySolverRepresentation extends AbstractArraySolverRepresentation implements AliasingArraySolverRepresentation {
    protected final Sint reservedId;
    protected final Constraint metadataConstraintForPotentialIds;
    protected final Set<IncrementalSolverState.ArrayRepresentation<ArraySolverRepresentation>> aliasedArrays;
    // Is Sarray containing this Sarray is completely initialized? If there is no containing Sarray, is false
    protected final boolean containingSarrayIsCompletelyInitialized;

    /**
     * New instance constructor
     * @param aic
     * @param level
     * @param potentialIds
     * @param symbolicArrayStates
     * @param containingSarrayIsCompletelyInitialized
     */
    protected AbstractAliasingArraySolverRepresentation(
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
        assert aic.getValueType().isArray();
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

    /**
     * Copy constructor
     * @param arrayId
     * @param reservedId
     * @param arrayLength
     * @param isNull
     * @param isCompletelyInitialized
     * @param level
     * @param metadataConstraintForPotentialIds
     * @param aliasedArrays
     * @param arrayHistorySolverRepresentation
     * @param containingSarrayIsCompletelyInitialized
     * @param valueType
     */
    protected AbstractAliasingArraySolverRepresentation(
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
    public Constraint getMetadataConstraintForPotentialIds() {
        return metadataConstraintForPotentialIds;
    }

}
