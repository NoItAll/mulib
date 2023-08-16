package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a non-array partner class object that potentially is represented by another pre-existing array.
 * Lazy initialization is supported.
 */
public class AliasingPartnerClassObjectSolverRepresentation extends AbstractPartnerClassObjectSolverRepresentation {
    /**
     * The reserved identifier, that this representation represents if it is not an alias of another partner class object.
     */
    protected final Sint reservedId;
    /**
     * The metadata constraint
     */
    protected final Constraint metadataConstraintForPotentialIds;
    /**
     * The representation of potentially aliased objects, if this is not a new instance
     */
    protected final List<IncrementalSolverState.PartnerClassObjectRepresentation<PartnerClassObjectSolverRepresentation>>
            aliasedObjects;
    /**
     * Whether or not this cannot be a new instance or is a pure alias
     */
    protected final boolean cannotBeNewInstance;

    /**
     * Constructs a new representation
     * @param config The configuration
     * @param sps The construct maintaining object representations for potential lazy initialization of fields
     * @param asr The construct maintaining array representations for potential lazy initialization of fields
     * @param pic The constraint initializing this representation
     * @param level The level this representation is initialized for
     * @param potentialIds The identifiers of potential aliasing targets
     * @param cannotBeNewInstance Whether or not this cannot be a new instance or is a pure alias
     */
    AliasingPartnerClassObjectSolverRepresentation(
            MulibConfig config,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr,
            PartnerClassObjectInitializationConstraint pic,
            int level,
            Set<Sint> potentialIds,
            boolean cannotBeNewInstance) {
        super(config, sps, asr, pic, level);
        this.reservedId = pic.getReservedId();
        assert getId() instanceof SymNumericExpressionSprimitive;
        assert reservedId instanceof ConcSnumber;
//        assert potentialIds != null && potentialIds.size() > 0 : "There always must be at least one potential aliasing candidate";
        this.aliasedObjects = new ArrayList<>(); // Is filled in getMetadataConstraintForPotentialIds
        this.cannotBeNewInstance = cannotBeNewInstance;
        this.metadataConstraintForPotentialIds = getMetadataConstraintForPotentialIds(potentialIds);
    }

    /**
     * Constructor for generating an AliasingPartnerClassObject lazily
     * @param config The configuration
     * @param id The symbolic identifier that can either be reserved identifier, or the identifier of an aliasing target
     * @param isNull Whether an object is represented that is potentially null
     * @param clazz The class represented by this representation
     * @param sps The construct maintaining object representations for potential lazy initialization of fields
     * @param asr The construct maintaining array representations for potential lazy initialization of fields
     * @param level The level this representation is created for
     * @param reservedId The reserved identifier
     * @param potentialIds The identifiers of the aliasing targets
     * @param cannotBeNewInstance Whether or not this can be a new instance, i.e., is not a pure alias
     */
    protected AliasingPartnerClassObjectSolverRepresentation(
            MulibConfig config,
            Sint id,
            Sbool isNull,
            Class<?> clazz,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr,
            int level,
            Sint reservedId,
            Set<Sint> potentialIds,
            boolean cannotBeNewInstance) {
        super(config, id, isNull, clazz, true, sps, asr, level);
        this.reservedId = reservedId;
        assert getId() instanceof SymNumericExpressionSprimitive;
//        assert potentialIds != null && potentialIds.size() > 0 : "There always must be at least one potential aliasing candidate";
        this.aliasedObjects = new ArrayList<>(); // Is filled in getMetadataConstraintForPotentialIds
        this.cannotBeNewInstance = cannotBeNewInstance;
        this.metadataConstraintForPotentialIds = getMetadataConstraintForPotentialIds(potentialIds);
    }

    private Constraint getMetadataConstraintForPotentialIds(
            Set<Sint> potentialIds) {
        boolean canBeNull;
        Constraint metadataEqualsDependingOnId;
        if (cannotBeNewInstance) {
            // For now, we set canBeNull to false; - this depends on the concrete content of the array/aliased PCOs
            canBeNull = false;
            metadataEqualsDependingOnId = Sbool.ConcSbool.FALSE;
        } else {
            canBeNull = config.ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL;
            metadataEqualsDependingOnId = Eq.newInstance(id, reservedId);
        }
        for (Sint id : potentialIds) {
            if (id == Sint.ConcSint.MINUS_ONE) {
                canBeNull = true;
                continue;
            }
            IncrementalSolverState.PartnerClassObjectRepresentation<PartnerClassObjectSolverRepresentation> pr =
                    sps.getRepresentationForId(id);
            assert pr != null : "All partner class objects for aliasingPotentialIds must be not null!";
            aliasedObjects.add(pr);
            PartnerClassObjectSolverRepresentation pcosr = pr.getNewestRepresentation();
            Constraint idsEqual = Eq.newInstance(id, this.id);
            Constraint isNullsEqual = Or.newInstance(
                    And.newInstance(isNull, pcosr.isNull()),
                    And.newInstance(Not.newInstance(isNull), Not.newInstance(pcosr.isNull()))
            );
            Constraint idEqualityImplies = And.newInstance(idsEqual, isNullsEqual);

            metadataEqualsDependingOnId = Or.newInstance(metadataEqualsDependingOnId, idEqualityImplies);
        }
        if (canBeNull) {
            metadataEqualsDependingOnId =
                    Or.newInstance(
                            metadataEqualsDependingOnId,
                            And.newInstance(
                                    Eq.newInstance(id, Sint.ConcSint.MINUS_ONE),
                                    isNull
                            )
                    );
        }
        assert metadataEqualsDependingOnId != Sbool.ConcSbool.FALSE;
        sps.addMetadataConstraint(metadataEqualsDependingOnId);
        return metadataEqualsDependingOnId;
    }

    /**
     * Copy constructor
     * @param apcor To-copy
     * @param level The level to create this copy for
     */
    AliasingPartnerClassObjectSolverRepresentation(AliasingPartnerClassObjectSolverRepresentation apcor, int level) {
        super(apcor, level);
        this.reservedId = apcor.reservedId;
        this.metadataConstraintForPotentialIds = apcor.metadataConstraintForPotentialIds;
        this.aliasedObjects = new ArrayList<>(apcor.aliasedObjects);
        this.cannotBeNewInstance = apcor.cannotBeNewInstance;
    }

    @Override
    public PartnerClassObjectSolverRepresentation copyForNewLevel(int level) {
        return new AliasingPartnerClassObjectSolverRepresentation(this, level);
    }

    @Override @SuppressWarnings("unchecked")
    public Set<Sint> getPartnerClassIdsKnownToBePossiblyContainedInField(String fieldName, boolean initializeSelfIfCanBeNew) {
        assert PartnerClass.class.isAssignableFrom(fieldToType.get(fieldName));
        Set<Sint> result;
        if (!cannotBeNewInstance) {
            // Can be new instance
            if (initializeSelfIfCanBeNew) {
                PartnerClassObjectSolverRepresentation potentiallyNewThis =
                        lazilyGenerateAndSetPartnerClassFieldIfNeeded(fieldName);
                if (potentiallyNewThis != this) {
                    return potentiallyNewThis.getPartnerClassIdsKnownToBePossiblyContainedInField(fieldName, initializeSelfIfCanBeNew);
                }
            }
            if (_fieldIsSet(fieldName)) {
                result = (Set<Sint>) fieldToRepresentation.get(fieldName).getValuesKnownToPossiblyBeContainedInArray(true);
            } else {
                result = new HashSet<>();
            }
        } else {
            result = new HashSet<>();
        }

        for (IncrementalSolverState.PartnerClassObjectRepresentation<PartnerClassObjectSolverRepresentation> r : aliasedObjects) {
            PartnerClassObjectSolverRepresentation pr = r.getNewestRepresentation();
            PartnerClassObjectSolverRepresentation potentiallyNewPr =
                    pr.lazilyGenerateAndSetPartnerClassFieldIfNeeded(fieldName);
            assert potentiallyNewPr._fieldIsSet(fieldName);
            // Since the aliasing graph does not have any cycles, this will not lead to an endless recursion
            result.addAll(potentiallyNewPr.getPartnerClassIdsKnownToBePossiblyContainedInField(fieldName, true));
        }

        return result;
    }


    @Override
    protected PartnerClassObjectSolverRepresentation lazilyGeneratePartnerClassObjectForField(String field) {
        Set<Sint> potentialIds = getPartnerClassIdsKnownToBePossiblyContainedInField(field, false);
        Sint id = Sint.newInputSymbolicSint();
        PartnerClassObjectSolverRepresentation result =
                new AliasingPartnerClassObjectSolverRepresentation(
                        config,
                        id,
                        !_fieldIsSet(field) ?
                                config.ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL ? Sbool.newInputSymbolicSbool() : Sbool.ConcSbool.FALSE
                                :
                                partnerClassFieldCanPotentiallyContainNull(field) ?
                                    Sbool.newInputSymbolicSbool()
                                    :
                                    Sbool.ConcSbool.FALSE,
                        fieldToType.get(field),
                        sps,
                        asr,
                        sps.getCurrentLevel(),
                        Sint.concSint(getNextUntrackedReservedId()),
                        potentialIds,
                        this.cannotBeNewInstance
                );
        sps.addRepresentationForId(id, result, sps.getCurrentLevel());
        return result;
    }

    @Override
    protected ArraySolverRepresentation lazilyGenerateArrayForField(String field) {
        Class<?> typeOfField = fieldToType.get(field);
        Sint id = Sint.newInputSymbolicSint();
        Set<Sint> potentialIds = getPartnerClassIdsKnownToBePossiblyContainedInField(field, false);
        Sbool isNull = partnerClassFieldCanPotentiallyContainNull(field) ?
                Sbool.newInputSymbolicSbool()
                :
                Sbool.ConcSbool.FALSE;
        assert !typeOfField.isArray();
        ArraySolverRepresentation result = Sarray.SarraySarray.class.isAssignableFrom(typeOfField) ?
                new AliasingPartnerClassArraySolverRepresentation(
                        config,
                        id,
                        Sint.newInputSymbolicSint(),
                        isNull,
                        typeOfField,
                        true,
                        sps.getCurrentLevel(),
                        Sint.concSint(getNextUntrackedReservedId()),
                        sps,
                        asr,
                        this.cannotBeNewInstance,
                        false,
                        false,
                        potentialIds
                )
                :
                new AliasingPrimitiveValuedArraySolverRepresentation(
                        config,
                        id,
                        Sint.newInputSymbolicSint(),
                        isNull,
                        typeOfField,
                        true,
                        sps.getCurrentLevel(),
                        Sint.concSint(getNextUntrackedReservedId()),
                        asr,
                        this.cannotBeNewInstance,
                        false,
                        false,
                        potentialIds
                );

        asr.addRepresentationForId(id, result, sps.getCurrentLevel());
        return result;
    }

    @Override
    protected Constraint _getField(Constraint guard, String fieldName, Sprimitive value) {
        if (guard instanceof Sbool.ConcSbool && ((Sbool.ConcSbool) guard).isFalse()) {
            return Sbool.ConcSbool.TRUE;
        }
        Constraint joinedGetfieldConstraint = Sbool.ConcSbool.TRUE;
        if (!cannotBeNewInstance) {
            AliasingPartnerClassObjectSolverRepresentation potentiallyNewThis =
                    (AliasingPartnerClassObjectSolverRepresentation) lazilyGenerateAndSetPartnerClassFieldIfNeeded(fieldName);
            if (potentiallyNewThis != this) {
                return potentiallyNewThis._getField(guard, fieldName, value);
            }
            joinedGetfieldConstraint = And.newInstance(
                    joinedGetfieldConstraint,
                    this.fieldToRepresentation
                            .get(fieldName)
                            .select(
                                    And.newInstance(guard, Eq.newInstance(id, reservedId)),
                                    Sint.ConcSint.ZERO,
                                    value,
                                    true,
                                    config.ALIASING_FOR_FREE_OBJECTS,
                                    fieldToType.get(fieldName),
                                    false
                            )
            );
        }
        for (IncrementalSolverState.PartnerClassObjectRepresentation<PartnerClassObjectSolverRepresentation> pr : aliasedObjects) {
            PartnerClassObjectSolverRepresentation psr = getAliasLevelSafe(pr);
            assert psr.getLevel() >= level;
            Constraint partialConstraint = psr.getField(And.newInstance(guard, Eq.newInstance(id, psr.getId())), fieldName, value);
            joinedGetfieldConstraint = And.newInstance(joinedGetfieldConstraint, partialConstraint);
        }
        return joinedGetfieldConstraint;
    }

    @Override
    protected void _putField(Constraint guard, String fieldName, Sprimitive value) {
        if (guard instanceof Sbool.ConcSbool && ((Sbool.ConcSbool) guard).isFalse()) {
            return;
        }
        if (!cannotBeNewInstance) {
            AliasingPartnerClassObjectSolverRepresentation potentiallyNewThis =
                    (AliasingPartnerClassObjectSolverRepresentation) lazilyGenerateAndSetPartnerClassFieldIfNeeded(fieldName);
            if (potentiallyNewThis != this) {
                potentiallyNewThis._putField(guard, fieldName, value);
                return;
            }
            fieldToRepresentation
                    .get(fieldName)
                    .store(And.newInstance(guard, Eq.newInstance(id, reservedId)), Sint.ConcSint.ZERO, value);
        }
        for (IncrementalSolverState.PartnerClassObjectRepresentation<PartnerClassObjectSolverRepresentation> pr : aliasedObjects) {
            PartnerClassObjectSolverRepresentation psr = getAliasLevelSafe(pr);
            assert psr.getLevel() >= level;
            psr.putField(And.newInstance(guard, Eq.newInstance(id, psr.getId())), fieldName, value);
        }
    }

    private PartnerClassObjectSolverRepresentation getAliasLevelSafe(
            IncrementalSolverState.PartnerClassObjectRepresentation<PartnerClassObjectSolverRepresentation> pr) {
        PartnerClassObjectSolverRepresentation result = pr.getNewestRepresentation();
        if (result.getLevel() < level) {
            // We allow larger levels. This can happen for safe operations, such as getting the values known
            // to be contained in the array
            result = result.copyForNewLevel(level);
            pr.addNewRepresentation(result, level);
        }
        return result;
    }


    @Override
    public String toString() {
        return String.format("AliasingPCORep[%s]{reservedId=%s, aliasingTargets=%s, fieldRep=%s, cannotBeNewInstance=%s}",
                id, reservedId, aliasedObjects.stream().map(o -> o.getNewestRepresentation().getId()).collect(Collectors.toList()),
                fieldToRepresentation, cannotBeNewInstance
        );
    }
}
