package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an array with Sint --> Sprimitive. Is also used to symbolically represent an array of arrays
 * using the IDs of the respective Sarrays. Is enhanced with the capabilities to treat aliasing, i.e.,
 * this array could be a new array or a pre-existing array. It is expected that the ID of this array is symbolic
 * so that it can in fact represent another array.
 */
public class AliasingPrimitiveValuedArraySolverRepresentation extends AbstractArraySolverRepresentation implements AliasingArraySolverRepresentation {

    protected final Sint reservedId;
    protected final Constraint metadataConstraintForPotentialIds;
    protected final Set<IncrementalSolverState.PartnerClassObjectRepresentation<ArraySolverRepresentation>> aliasedArrays;
    // Is Sarray containing this Sarray is completely initialized? If there is no containing Sarray, is false
    protected final boolean containingSarrayIsCompletelyInitialized;

    public AliasingPrimitiveValuedArraySolverRepresentation(
            MulibConfig config,
            final ArrayInitializationConstraint aic,
            final int level,
            final Set<Sint> potentialIds,
            final IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> symbolicArrayStates,
            boolean containingSarrayIsCompletelyInitialized) {
        super(config, aic, level);
        this.containingSarrayIsCompletelyInitialized = containingSarrayIsCompletelyInitialized;
        this.reservedId = aic.getReservedId();
        assert arrayId instanceof SymNumericExpressionSprimitive;
        assert reservedId instanceof ConcSnumber;
        assert potentialIds != null && potentialIds.size() > 0 : "There always must be at least one potential aliasing candidate";
        this.aliasedArrays = new HashSet<>(); // Is filled in getMetadataConstraintForPotentialIds
        this.metadataConstraintForPotentialIds =
                getMetadataConstraintForPotentialIds(potentialIds, symbolicArrayStates);
    }

    private Constraint getMetadataConstraintForPotentialIds(
            Set<Sint> potentialIds,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> symbolicArrayStates) {
        Constraint metadataEqualsDependingOnId;
        boolean mustBeAbleToBeNull = false;
        if (containingSarrayIsCompletelyInitialized) {
            // If the array is completely initialized, we do not need to enforce any defaults
            // Everything is already represented, including null values
            metadataEqualsDependingOnId = Sbool.ConcSbool.FALSE;
        } else {
            // If the array is not completely initialized, we need to differentiate two cases
            // 1: defaultIsSymbolic
            if (defaultIsSymbolic) {
                // If defaultIsSymbolic, we might address a completely new array. This "array" might still be null
                // and will be initialized with isNull as an unrestricted SymSbool.
                metadataEqualsDependingOnId = Eq.newInstance(arrayId, reservedId);
                if (config.ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL) {
                    mustBeAbleToBeNull = true;
                }
            } else {
                // If the default is the Java-usual semantics, we must add that the ID can be null.
                // In this case, the arrayId might be MINUS_ONE indicating that the array is null.
                // We do this below
                mustBeAbleToBeNull = true;
                metadataEqualsDependingOnId = Sbool.ConcSbool.FALSE;
            }
        }
        for (Sint id : potentialIds) {
            if (id == Sint.ConcSint.MINUS_ONE) {
                // We skip this case for now (there is no ArrayRepresentation of the null-array)
                mustBeAbleToBeNull = true;
                continue;
            }
            IncrementalSolverState.PartnerClassObjectRepresentation<ArraySolverRepresentation> ar =
                    symbolicArrayStates.getRepresentationForId(id);
            assert ar != null : "All Sarrays for aliasingPotentialIds must be not null!";
            aliasedArrays.add(ar);
            ArraySolverRepresentation asr = ar.getNewestRepresentation();
            Constraint idsEqual = Eq.newInstance(id, arrayId);
            Constraint isNullsEqual = Or.newInstance(
                    And.newInstance(isNull, asr.getIsNull()),
                    And.newInstance(Not.newInstance(isNull), Not.newInstance(asr.getIsNull()))
            );
            Constraint lengthsEqual = Eq.newInstance(length, asr.getLength());
            Constraint idEqualityImplies = And.newInstance(
                    idsEqual,
                    isNullsEqual,
                    lengthsEqual
            );
            metadataEqualsDependingOnId = Or.newInstance(metadataEqualsDependingOnId, idEqualityImplies);
        }

        if (mustBeAbleToBeNull) {
            // If the containing array can contain null, this is indicated by this array's ID being MINUS_ONE.
            // Thus, if the ID is MINUS_ONE, we imply isNull
            metadataEqualsDependingOnId =
                    Or.newInstance(
                            metadataEqualsDependingOnId,
                            And.newInstance(
                                    Eq.newInstance(arrayId, Sint.ConcSint.MINUS_ONE),
                                    isNull
                            )
                    );
        }


        return metadataEqualsDependingOnId;
    }

    protected AliasingPrimitiveValuedArraySolverRepresentation(
            AliasingPrimitiveValuedArraySolverRepresentation apvasr,
            int level) {
        super(apvasr, level);
        this.reservedId = apvasr.reservedId;
        this.metadataConstraintForPotentialIds = apvasr.metadataConstraintForPotentialIds;
        this.containingSarrayIsCompletelyInitialized = apvasr.containingSarrayIsCompletelyInitialized;
        this.aliasedArrays = apvasr.aliasedArrays;
        for (IncrementalSolverState.PartnerClassObjectRepresentation<ArraySolverRepresentation> ar : apvasr.aliasedArrays) {
            ArraySolverRepresentation asr = ar.getNewestRepresentation();
            if (asr.getLevel() != level) {
                ar.addNewRepresentation(asr.copyForNewLevel(level), level);
            }
        }
    }

    @Override
    protected Constraint _select(Constraint guard, Sint index, Sprimitive selectedValue) {
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
                            isCompletelyInitialized,
                            this instanceof IArrayArraySolverRepresentation && defaultIsSymbolic ?
                                    false // The metadata constraint of the selected sarray will already validly restrict the id values
                                    :
                                    canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault
                    );
            joinedSelectConstraint = ownConstraint;
        }
        for (IncrementalSolverState.PartnerClassObjectRepresentation<ArraySolverRepresentation> ar : aliasedArrays) {
            ArraySolverRepresentation asr = ar.getNewestRepresentation();
            Constraint partialSelectConstraint = asr.select(And.newInstance(guard, Eq.newInstance(arrayId, asr.getArrayId())), index, selectedValue);
            joinedSelectConstraint = And.newInstance(joinedSelectConstraint, partialSelectConstraint);
        }
        return joinedSelectConstraint;
    }

    @Override
    protected void _store(Constraint guard, Sint index, Sprimitive storedValue) {
        if (guard instanceof Sbool.ConcSbool && ((Sbool.ConcSbool) guard).isFalse()) {
            return;
        }

        for (IncrementalSolverState.PartnerClassObjectRepresentation<ArraySolverRepresentation> ar : aliasedArrays) {
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
        return new AliasingPrimitiveValuedArraySolverRepresentation(
                this,
                level
        );
    }

    @Override
    public Constraint getMetadataConstraintForPotentialIds() {
        return metadataConstraintForPotentialIds;
    }
}
