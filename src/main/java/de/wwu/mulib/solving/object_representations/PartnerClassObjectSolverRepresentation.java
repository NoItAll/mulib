package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectInitializationConstraint;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.Set;

public interface PartnerClassObjectSolverRepresentation {

    static PartnerClassObjectSolverRepresentation newInstance(
            MulibConfig mc,
            PartnerClassObjectInitializationConstraint pc,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> symbolicArrayStates,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> symbolicPartnerClassObjectStates,
            int level) {
        PartnerClassObjectSolverRepresentation result;
        if (pc.getType() == PartnerClassObjectInitializationConstraint.Type.SIMPLE_PARTNER_CLASS_OBJECT) {
            result = new SimplePartnerClassObjectSolverRepresentation(mc, symbolicPartnerClassObjectStates, symbolicArrayStates, pc, level);
        } else if (pc.getType() == PartnerClassObjectInitializationConstraint.Type.PARTNER_CLASS_OBJECT_IN_SARRAY) {
            ArraySolverRepresentation asr =
                    symbolicArrayStates
                            .getRepresentationForId(pc.getContainingPartnerClassObjectId())
                            .getNewestRepresentation();
            assert asr instanceof PartnerClassArraySolverRepresentation;
            PartnerClassArraySolverRepresentation pasr = (PartnerClassArraySolverRepresentation) asr;
            Set<Sint> aliasedPcos = pasr.getValuesKnownToPossiblyBeContainedInArray();
            result = new AliasingPartnerClassObjectSolverRepresentation(mc, symbolicPartnerClassObjectStates, symbolicArrayStates, pc, level, aliasedPcos, asr.isCompletelyInitialized());
        } else if (pc.getType() == PartnerClassObjectInitializationConstraint.Type.ALIASED_PARTNER_CLASS_OBJECT) {
            result = new AliasingPartnerClassObjectSolverRepresentation(mc, symbolicPartnerClassObjectStates, symbolicArrayStates, pc, level, pc.getPotentialIds(), false);
        } else {
            assert pc.getType() == PartnerClassObjectInitializationConstraint.Type.PARTNER_CLASS_OBJECT_IN_PARTNER_CLASS_OBJECT;
            PartnerClassObjectSolverRepresentation psr =
                    symbolicPartnerClassObjectStates
                            .getRepresentationForId(pc.getContainingPartnerClassObjectId())
                            .getNewestRepresentation();
            assert psr != null;
            Set<Sint> ids = psr.getPartnerClassIdsKnownToBePossiblyContainedInField(pc.getFieldName(), true);
            result = new AliasingPartnerClassObjectSolverRepresentation(mc, symbolicPartnerClassObjectStates, symbolicArrayStates, pc, level, ids, false); //// TODO is false sufficient or how to determine whether new instance or not?
        }
        return result;
    }

    Constraint getField(String fieldName, Sprimitive value);

    void putField(String fieldName, Sprimitive value);

    Constraint getField(Constraint guard, String fieldName, Sprimitive value);

    void putField(Constraint guard, String fieldName, Sprimitive value);

    Sint getId();
    Sbool isNull();
    int getLevel();
    Class<?> getClazz();

    boolean defaultIsSymbolic();

    PartnerClassObjectSolverRepresentation copyForNewLevel(int level);

    Set<Sint> getPartnerClassIdsKnownToBePossiblyContainedInField(String fieldName, boolean initializeSelfIfCanBeNew);

    PartnerClassObjectSolverRepresentation lazilyGenerateAndSetPartnerClassFieldIfNeeded(String field);

    boolean partnerClassFieldCanPotentiallyContainNull(String field);

    boolean _fieldIsSet(String field);
}
