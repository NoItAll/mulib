package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents an array with Sint --> Sprimitive. Is also used to symbolically represent an array of arrays
 * using the IDs of the respective Sarrays. Is enhanced with the capabilities to treat aliasing, i.e.,
 * this array could be a new array or a pre-existing array. It is expected that the ID of this array is symbolic
 * so that it can in fact represent another array.
 */
public class AliasingPrimitiveValuedArraySolverRepresentation extends AbstractArraySolverRepresentation implements AliasingArraySolverRepresentation {

    /**
     * The identifier reserved for this representation IF the representation is a new one
     */
    protected final Sint reservedId;
    /**
     * The metadata constraint
     */
    protected final Constraint metadataConstraintForPotentialIds;
    /**
     * The potential aliasing targets
     */
    protected final Set<IncrementalSolverState.PartnerClassObjectRepresentation<ArraySolverRepresentation>> aliasedArrays;
    /**
     * Whether or not this is a pure alias, i.e., no own instance, or can also represent a new instance
     */
    protected final boolean cannotBeNewInstance;

    /**
     * Constructs a new instance
     * @param config The configuration
     * @param aic The initialization constraint for constructing this representation. Also contains the reservedIdentifier
     * @param level The level this representation is initialized for
     * @param potentialIds The potential aliases
     * @param symbolicArrayStates The construct containing the array representations. Is used to formulate
     *                            the metadata constraint
     * @param cannotBeNewInstance Whether or not this is a pure alias
     */
    AliasingPrimitiveValuedArraySolverRepresentation(
            MulibConfig config,
            final ArrayInitializationConstraint aic,
            final int level,
            final Set<Sint> potentialIds,
            final IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> symbolicArrayStates,
            boolean cannotBeNewInstance) {
        super(config, aic, level);
        this.cannotBeNewInstance = cannotBeNewInstance;
        this.reservedId = aic.getReservedId();
        assert arrayId instanceof SymNumericExpressionSprimitive;
        assert reservedId instanceof ConcSnumber;
//        assert potentialIds != null && potentialIds.size() > 0 : "There always must be at least one potential aliasing candidate";
        this.aliasedArrays = new HashSet<>(); // Is filled in getMetadataConstraintForPotentialIds
        this.metadataConstraintForPotentialIds =
                getMetadataConstraintForPotentialIds(potentialIds, symbolicArrayStates);
        if (cannotBeNewInstance) {
            // Set by superclass:
            assert !isCompletelyInitialized;
            // If this is a pure alias, this array is completely initialized, if its potential aliases are
            boolean ici = true;
            boolean unsd = false;
            for (Sint i : potentialIds) {
                if (i == Sint.ConcSint.MINUS_ONE) {
                    continue;
                }
                ArraySolverRepresentation asr = symbolicArrayStates.getRepresentationForId(i).getNewestRepresentation();
                if (!asr.isCompletelyInitialized()) {
                    ici = false;
                }
                if (asr.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault()) {
                    unsd = true;
                }
            }

            isCompletelyInitialized = ici;
            canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault = unsd;
        }
    }

    /**
     * Constructor for generating lazily
     * @param config The configuration
     * @param id The identifier
     * @param length The length
     * @param isNull Whether the array is null
     * @param valueType The component type
     * @param defaultIsSymbolic Whether uninitialized indexes return a new symbolic index
     * @param level The level
     * @param reservedId The reserved identifier
     * @param symbolicArrayStates The construct containing the array representations. Is used to formulate
     *                            the metadata constraint
     * @param cannotBeNewInstance Whether or not this is a pure alias
     * @param isCompletelyInitialized Whether we know for sure that the content of this array is completely known
     * @param canPotentiallyContainCurrentlyUnrepresentedDefaults Whether or not
     * @param potentialIds The identifiers of potentially symbolic aliasing targets
     */
    protected AliasingPrimitiveValuedArraySolverRepresentation(
            MulibConfig config,
            Sint id,
            Sint length,
            Sbool isNull,
            Class<?> valueType,
            boolean defaultIsSymbolic,
            int level,
            Sint reservedId,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> symbolicArrayStates,
            boolean cannotBeNewInstance,
            boolean isCompletelyInitialized,
            boolean canPotentiallyContainCurrentlyUnrepresentedDefaults,
            Set<Sint> potentialIds) {
        super(config, id, length, isNull, valueType, defaultIsSymbolic, isCompletelyInitialized, canPotentiallyContainCurrentlyUnrepresentedDefaults, level);
        this.reservedId = reservedId;
        this.aliasedArrays = new HashSet<>();
        this.cannotBeNewInstance = cannotBeNewInstance;
        this.metadataConstraintForPotentialIds =
                getMetadataConstraintForPotentialIds(potentialIds, symbolicArrayStates);

    }

    private Constraint getMetadataConstraintForPotentialIds(
            Set<Sint> potentialIds,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> symbolicArrayStates) {
        Constraint metadataEqualsDependingOnId;
        boolean canBeNull = false;
        if (cannotBeNewInstance) {
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
                    canBeNull = true;
                }
            } else {
                // If the default is the Java-usual semantics, we must add that the ID can be null.
                // In this case, the arrayId might be MINUS_ONE indicating that the array is null.
                // We do this below
                canBeNull = true;
                metadataEqualsDependingOnId = Sbool.ConcSbool.FALSE;
            }
        }
        for (Sint id : potentialIds) {
            if (id == Sint.ConcSint.MINUS_ONE) {
                // We skip this case for now (there is no ArrayRepresentation of the null-array)
                canBeNull = true;
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

        if (canBeNull) {
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

        assert metadataEqualsDependingOnId != Sbool.ConcSbool.FALSE;
        symbolicArrayStates.addMetadataConstraint(metadataEqualsDependingOnId);
        return metadataEqualsDependingOnId;
    }

    /**
     * Copy constructor
     * @param apvasr To-copy
     * @param level The level to copy for
     */
    AliasingPrimitiveValuedArraySolverRepresentation(
            AliasingPrimitiveValuedArraySolverRepresentation apvasr,
            int level) {
        super(apvasr, level);
        this.reservedId = apvasr.reservedId;
        this.metadataConstraintForPotentialIds = apvasr.metadataConstraintForPotentialIds;
        this.cannotBeNewInstance = apvasr.cannotBeNewInstance;
        this.aliasedArrays = new HashSet<>(apvasr.aliasedArrays);
    }

    @Override
    protected Constraint _select(Constraint guard, Sint index, Sprimitive selectedValue) {
        if (guard instanceof Sbool.ConcSbool && ((Sbool.ConcSbool) guard).isFalse()) {
            return Sbool.ConcSbool.TRUE;
        }
        Constraint joinedSelectConstraint;
        if (cannotBeNewInstance) {
            joinedSelectConstraint = Sbool.ConcSbool.TRUE;
        } else {
            // currentRepresentation here is the representation for this.reservedId
            // The metadata constraint of the selected sarray will already validly restrict the id values
            joinedSelectConstraint = this.currentRepresentation.select(
                    And.newInstance(guard, Eq.newInstance(arrayId, reservedId)),
                    index,
                    selectedValue,
                    isCompletelyInitialized,
                    config.ALIASING_FOR_FREE_OBJECTS,
                    valueType,
                    !defaultIsSymbolic && canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault
            );
        }
        for (IncrementalSolverState.PartnerClassObjectRepresentation<ArraySolverRepresentation> ar : aliasedArrays) {
            ArraySolverRepresentation asr = getAliasLevelSafe(ar);
            assert level <= asr.getLevel();
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
            ArraySolverRepresentation asr = getAliasLevelSafe(ar);
            assert level <= asr.getLevel();
            asr.store(And.newInstance(guard, Eq.newInstance(arrayId, asr.getArrayId())), index, storedValue);
        }
        if (!cannotBeNewInstance) {
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

    final ArraySolverRepresentation getAliasLevelSafe(
            IncrementalSolverState.PartnerClassObjectRepresentation<ArraySolverRepresentation> ar) {
        ArraySolverRepresentation result = ar.getNewestRepresentation();
        if (result.getLevel() < level) {
            // We allow larger levels. This can happen for safe operations, such as getting the values known
            // to be contained in the array
            result = result.copyForNewLevel(level);
            ar.addNewRepresentation(result, level);
        }
        return result;
    }

    @Override
    public Collection<Sint> getAliasedIds() {
        return aliasedArrays.stream().map(a -> a.getNewestRepresentation().getArrayId()).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("AliasingArrayRep[%s]{reservedId=%s, length=%s, isNull=%s, aliasingTargets=%s" +
                        ", currentRepresentation=%s, isPrimitive=%s, cannotBeNewInstance=%s}",
                arrayId, reservedId, length, isNull, getAliasedIds(),
                currentRepresentation, !(this instanceof AliasingPartnerClassArraySolverRepresentation),
                cannotBeNewInstance
        );
    }
}
