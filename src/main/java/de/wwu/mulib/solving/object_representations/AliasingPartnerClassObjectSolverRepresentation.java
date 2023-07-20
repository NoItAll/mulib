package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.*;
import java.util.stream.Collectors;

public class AliasingPartnerClassObjectSolverRepresentation extends AbstractPartnerClassObjectSolverRepresentation {
    protected final Sint reservedId;
    protected final Constraint metadataConstraintForPotentialIds;
    protected final List<IncrementalSolverState.PartnerClassObjectRepresentation<PartnerClassObjectSolverRepresentation>> aliasedObjects;
    protected final boolean cannotBeNewInstance;

    protected AliasingPartnerClassObjectSolverRepresentation(
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
        assert potentialIds != null && potentialIds.size() > 0 : "There always must be at least one potential aliasing candidate";
        this.aliasedObjects = new ArrayList<>(); // Is filled in getMetadataConstraintForPotentialIds
        this.cannotBeNewInstance = cannotBeNewInstance;
        this.metadataConstraintForPotentialIds = getMetadataConstraintForPotentialIds(potentialIds);
    }

    /**
     * Constructor for generating an AliasingPartnerClassObject lazily
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
            Set<Sint> potentialIds) {
        super(config, id, isNull, clazz, true, sps, asr, level);
        this.reservedId = reservedId;
        assert getId() instanceof SymNumericExpressionSprimitive;
        assert potentialIds != null && potentialIds.size() > 0 : "There always must be at least one potential aliasing candidate";
        this.aliasedObjects = new ArrayList<>(); // Is filled in getMetadataConstraintForPotentialIds
        this.metadataConstraintForPotentialIds = getMetadataConstraintForPotentialIds(potentialIds);
        // We have a fixed set of options here
        this.cannotBeNewInstance = true;
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
        return metadataEqualsDependingOnId;
    }

    protected AliasingPartnerClassObjectSolverRepresentation(AliasingPartnerClassObjectSolverRepresentation apcor, int level) {
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

    @Override
    public Set<Sint> getPartnerClassIdsKnownToBePossiblyContainedInField(String fieldName) {
        assert PartnerClass.class.isAssignableFrom(fieldToType.get(fieldName));
        return _getPartnerClassIdsKnownToBePossiblyContainedInField(fieldName, false);
    }

    @SuppressWarnings("unchecked")
    public Set<Sint> _getPartnerClassIdsKnownToBePossiblyContainedInField(String fieldName, boolean initalizeLazily) {
        Set<Sint> result;
        if (!cannotBeNewInstance) {
            // Can be new instance
            if (initalizeLazily) {
                lazilyGenerateAndSetPartnerClassFieldIfNeeded(fieldName);
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
            pr.lazilyGenerateAndSetPartnerClassFieldIfNeeded(fieldName);
            assert pr._fieldIsSet(fieldName);
            // Since the aliasing graph does not have any cycles, this will not lead to an endless recursion
            result.addAll(pr.getPartnerClassIdsKnownToBePossiblyContainedInField(fieldName));
        }

        return result;
    }

    @Override
    protected PartnerClassObjectSolverRepresentation lazilyGeneratePartnerClassObjectForField(String field) {
        Set<Sint> potentialIds = _getPartnerClassIdsKnownToBePossiblyContainedInField(field, false);
        Sint id = Sint.newInputSymbolicSint();
        PartnerClassObjectSolverRepresentation result =
                new AliasingPartnerClassObjectSolverRepresentation(
                        config,
                        id,
                        partnerClassFieldCanPotentiallyContainNull(field) ?
                                Sbool.newInputSymbolicSbool()
                                :
                                Sbool.ConcSbool.FALSE,
                        fieldToType.get(field),
                        sps,
                        asr,
                        level,
                        Sint.concSint(getNextUntrackedReservedId()),
                        potentialIds
                );
        sps.addRepresentationForId(id, result, level);
        return result;
    }

    @Override
    protected ArraySolverRepresentation lazilyGenerateArrayForField(String field) {
        Class<?> typeOfField = fieldToType.get(field);
        Sint id = Sint.newInputSymbolicSint();
        Set<Sint> potentialIds = _getPartnerClassIdsKnownToBePossiblyContainedInField(field, false);
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
                        level,
                        Sint.concSint(getNextUntrackedReservedId()),
                        sps,
                        asr,
                        true,
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
                        level,
                        Sint.concSint(getNextUntrackedReservedId()),
                        asr,
                        true,
                        false,
                        false,
                        potentialIds
                );

        asr.addRepresentationForId(id, result, level);
        return result;
    }

    @Override
    protected Constraint _getField(Constraint guard, String fieldName, Sprimitive value) {
        if (guard instanceof Sbool.ConcSbool && ((Sbool.ConcSbool) guard).isFalse()) {
            return Sbool.ConcSbool.TRUE;
        }
        Constraint joinedGetfieldConstraint = Sbool.ConcSbool.TRUE;
        for (IncrementalSolverState.PartnerClassObjectRepresentation<PartnerClassObjectSolverRepresentation> pr : aliasedObjects) {
            PartnerClassObjectSolverRepresentation psr = getAliasLevelSafe(pr);
            assert psr.getLevel() >= level;
            Constraint partialConstraint = psr.getField(And.newInstance(guard, Eq.newInstance(id, psr.getId())), fieldName, value);
            joinedGetfieldConstraint = And.newInstance(joinedGetfieldConstraint, partialConstraint);
        }
        if (!cannotBeNewInstance) {
            lazilyGenerateAndSetPartnerClassFieldIfNeeded(fieldName);
            joinedGetfieldConstraint = And.newInstance(
                    joinedGetfieldConstraint,
                    this.fieldToRepresentation
                            .get(fieldName)
                            .select(And.newInstance(guard, Eq.newInstance(id, reservedId)), Sint.ConcSint.ZERO, value, true, false));
        }
        return joinedGetfieldConstraint;
    }

    @Override
    protected void _putField(Constraint guard, String fieldName, Sprimitive value) {
        if (guard instanceof Sbool.ConcSbool && ((Sbool.ConcSbool) guard).isFalse()) {
            return;
        }
        for (IncrementalSolverState.PartnerClassObjectRepresentation<PartnerClassObjectSolverRepresentation> pr : aliasedObjects) {
            PartnerClassObjectSolverRepresentation psr = getAliasLevelSafe(pr);
            assert psr.getLevel() >= level;
            psr.putField(And.newInstance(guard, Eq.newInstance(id, psr.getId())), fieldName, value);
        }
        if (!cannotBeNewInstance) {
            lazilyGenerateAndSetPartnerClassFieldIfNeeded(fieldName);
            fieldToRepresentation
                    .get(fieldName)
                    .store(And.newInstance(guard, Eq.newInstance(id, reservedId)), Sint.ConcSint.ZERO, value);
        }
    }

    public Constraint getMetadataConstraintForPotentialIds() {
        return metadataConstraintForPotentialIds;
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

    public Collection<Sint> getAliasedObjects() {
        return aliasedObjects.stream().map(o -> o.getNewestRepresentation().getId()).collect(Collectors.toList());
    }
}
