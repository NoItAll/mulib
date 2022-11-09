package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AliasingPartnerClassObjectRepresentation extends AbstractPartnerClassObjectRepresentation {
    protected final Sint reservedId;
    protected final Constraint metadataConstraintForPotentialIds;
    protected final List<IncrementalSolverState.PartnerClassObjectRepresentation<PartnerClassObjectSolverRepresentation>> aliasedObjects;
    protected final boolean cannotBeNewInstance;

    protected AliasingPartnerClassObjectRepresentation(
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
        this.metadataConstraintForPotentialIds = getMetadataConstraintForPotentialIds(potentialIds);
        this.cannotBeNewInstance = cannotBeNewInstance;
    }

    /**
     * Constructor for generating an AliasingPartnerClassObject lazily
     */
    protected AliasingPartnerClassObjectRepresentation(
            MulibConfig config,
            Sint id,
            Sbool isNull,
            Class<?> clazz,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr,
            int level,
            Sint reservedId,
            Set<Sint> potentialIds) {
        super(config, id, isNull, clazz, true, true, sps, asr, level);
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

    protected AliasingPartnerClassObjectRepresentation(AliasingPartnerClassObjectRepresentation apcor, int level) {
        super(apcor, level);
        this.reservedId = apcor.reservedId;
        this.metadataConstraintForPotentialIds = apcor.metadataConstraintForPotentialIds;
        this.aliasedObjects = apcor.aliasedObjects;
        this.cannotBeNewInstance = apcor.cannotBeNewInstance;
    }

    @Override
    public PartnerClassObjectSolverRepresentation copyForNewLevel(int level) {
        return new AliasingPartnerClassObjectRepresentation(this, level);
    }

    @Override
    public Set<Sint> getPartnerClassIdsKnownToBePossiblyContainedInField(String fieldName) {
        assert PartnerClass.class.isAssignableFrom(fieldToType.get(fieldName));
        return _getPartnerClassIdsKnownToBePossiblyContainedInField(fieldName, true);
    }

    @SuppressWarnings("unchecked")
    public Set<Sint> _getPartnerClassIdsKnownToBePossiblyContainedInField(String fieldName, boolean initalizeLazily) {
        Set<Sint> result;
        if (!cannotBeNewInstance) {
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
        PartnerClassObjectSolverRepresentation result =
                new AliasingPartnerClassObjectRepresentation(
                        config,
                        Sint.newInputSymbolicSint(),
                        partnerClassFieldCanContainNull(field) ?
                                Sbool.newInputSymbolicSbool()
                                :
                                Sbool.ConcSbool.FALSE,
                        fieldToType.get(field),
                        sps,
                        asr,
                        level,
                        Sint.ConcSint.ZERO,
                        potentialIds
                );
        return result;
    }

    @Override
    protected ArraySolverRepresentation lazilyGenerateArrayForField(String field) {
        Class<?> typeOfField = fieldToType.get(field);
        Set<Sint> potentialIds = _getPartnerClassIdsKnownToBePossiblyContainedInField(field, false);
        Sbool isNull = partnerClassFieldCanContainNull(field) ?
                Sbool.newInputSymbolicSbool()
                :
                Sbool.ConcSbool.FALSE;
        return typeOfField.getComponentType().isArray() ?
                new AliasingPartnerClassArraySolverRepresentation(
                        config,
                        Sint.newInputSymbolicSint(),
                        Sint.newInputSymbolicSint(),
                        isNull,
                        typeOfField,
                        true,
                        level,
                        Sint.ConcSint.ZERO, //// TODO
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
                        Sint.newInputSymbolicSint(),
                        Sint.newInputSymbolicSint(),
                        isNull,
                        typeOfField,
                        true,
                        level,
                        Sint.ConcSint.ZERO, //// TODO
                        asr,
                        true,
                        false,
                        false,
                        potentialIds
                );
    }

    @Override
    protected Constraint _getField(Constraint guard, String fieldName, Sprimitive value) {
        if (guard instanceof Sbool.ConcSbool && ((Sbool.ConcSbool) guard).isFalse()) {
            return Sbool.ConcSbool.TRUE;
        }
        Constraint joinedGetfieldConstraint = Sbool.ConcSbool.TRUE;
        for (IncrementalSolverState.PartnerClassObjectRepresentation<PartnerClassObjectSolverRepresentation> pr : aliasedObjects) {
            PartnerClassObjectSolverRepresentation psr = pr.getNewestRepresentation();
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
            PartnerClassObjectSolverRepresentation psr = pr.getNewestRepresentation();
            psr.putField(And.newInstance(guard, Eq.newInstance(id, psr.getId())), fieldName, value);
        }
        if (!cannotBeNewInstance) {
            lazilyGenerateAndSetPartnerClassFieldIfNeeded(fieldName);
            fieldToRepresentation
                    .get(fieldName)
                    .store(And.newInstance(guard, Eq.newInstance(id, reservedId)), Sint.ConcSint.ZERO, value);
        }
    }
}
