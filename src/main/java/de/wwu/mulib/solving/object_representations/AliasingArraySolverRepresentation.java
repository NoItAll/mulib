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

    public AliasingArraySolverRepresentation(
            final Sint arrayId,
            final Sint arrayLength,
            final Sbool isNull,
            final int level,
            final Sint reservedId,
            final Set<Sint> potentialIds,
            final IncrementalSolverState.SymbolicArrayStates<ArraySolverRepresentation> symbolicArrayStates) {
        super(arrayId, arrayLength, isNull, level, new ArrayHistorySolverRepresentation());
        assert arrayId instanceof SymNumericExpressionSprimitive;
        assert potentialIds != null && potentialIds.size() > 0 : "There always must be at least one potential aliasing candidate";
        this.aliasedArrays = new HashSet<>();
        Constraint reservedIdIsEqual = Eq.newInstance(arrayId, reservedId);
        Constraint metadataEqualsDependingOnId = reservedIdIsEqual;
        for (Sint id : potentialIds) {
            IncrementalSolverState.ArrayRepresentation<ArraySolverRepresentation> ar =
                    symbolicArrayStates.getArraySolverRepresentationForId(id);
            assert ar != null : "All Sarrays for aliasingPotentialIds must be not null!";
            aliasedArrays.add(ar);
            ArraySolverRepresentation asr = ar.getNewestRepresentation();
            Constraint idsEqual = Eq.newInstance(id, arrayId);
            Constraint isNullsEqual = Or.newInstance(And.newInstance(isNull, asr.getIsNull()), And.newInstance(Not.newInstance(isNull), Not.newInstance(asr.getIsNull())));
            Constraint lengthsEqual = Eq.newInstance(arrayLength, asr.getLength());
            Constraint idEqualityImplies = And.newInstance(
                    idsEqual,
                    isNullsEqual,
                    lengthsEqual
            );
            metadataEqualsDependingOnId = Or.newInstance(metadataEqualsDependingOnId, idEqualityImplies);
        }
        this.reservedId = reservedId;
        this.metadataConstraintForPotentialIds = metadataEqualsDependingOnId;
    }

    private AliasingArraySolverRepresentation(
            Sint arrayId,
            Sint reservedId,
            Sint arrayLength,
            Sbool isNull,
            int level,
            Constraint metadataConstraintForPotentialIds,
            Set<IncrementalSolverState.ArrayRepresentation<ArraySolverRepresentation>> aliasedArrays,
            ArrayHistorySolverRepresentation arrayHistorySolverRepresentation) {
        super(arrayId, arrayLength, isNull, level, arrayHistorySolverRepresentation);
        this.reservedId = reservedId;
        this.metadataConstraintForPotentialIds = metadataConstraintForPotentialIds;
        this.aliasedArrays = aliasedArrays;
        for (IncrementalSolverState.ArrayRepresentation<ArraySolverRepresentation> ar : aliasedArrays) {
            ArraySolverRepresentation asr = ar.getNewestRepresentation();
            if (asr.getLevel() != level) {
                ar.addNewRepresentation(asr.copyForNewLevel(level), level);
            }
        }
    }

    private static Constraint implies(Constraint c0, Constraint c1) {
        return Or.newInstance(Not.newInstance(c0), c1);
    }

    @Override
    public Constraint select(Constraint guard, Sint index, Sprimitive selectedValue) {
        if (guard instanceof Sbool.ConcSbool && ((Sbool.ConcSbool) guard).isFalse()) {
            return Sbool.ConcSbool.TRUE;
        }
        // currentRepresentation here is the representation for this.reservedId
        Constraint ownConstraint = this.currentRepresentation.select(And.newInstance(guard, Eq.newInstance(arrayId, reservedId)), index, selectedValue);
        Constraint joinedSelectConstraint = ownConstraint;
        for (IncrementalSolverState.ArrayRepresentation<ArraySolverRepresentation> ar : aliasedArrays) {
            ArraySolverRepresentation asr = ar.getNewestRepresentation();
            Constraint partialSelectConstraint = asr.select(And.newInstance(guard, Eq.newInstance(arrayId, asr.getArrayId())), index, selectedValue);
            joinedSelectConstraint = And.newInstance(joinedSelectConstraint, partialSelectConstraint);
        }
        Constraint result = joinedSelectConstraint;
        return result;
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
        // currentRepresentation here is the representation for this.reservedId
       this.currentRepresentation = this.currentRepresentation.store(And.newInstance(guard, Eq.newInstance(arrayId, reservedId)), index, storedValue);
    }

    @Override
    public AliasingArraySolverRepresentation copyForNewLevel(int level) {
        return new AliasingArraySolverRepresentation(
                arrayId,
                reservedId,
                length,
                isNull,
                level,
                metadataConstraintForPotentialIds,
                aliasedArrays,
                currentRepresentation.copy()
        );
    }

    @Override
    public Set<? extends Sprimitive> getPotentialValues() {
        Set<Sprimitive> result = currentRepresentation.getPotentialValues();

        for (IncrementalSolverState.ArrayRepresentation<ArraySolverRepresentation> ar : aliasedArrays) {
            ArraySolverRepresentation asr = ar.getNewestRepresentation();
            result.addAll(asr.getPotentialValues());
        }

        return result;
    }

    public Constraint getMetadataConstraintForPotentialIds() {
        return metadataConstraintForPotentialIds;
    }
}
