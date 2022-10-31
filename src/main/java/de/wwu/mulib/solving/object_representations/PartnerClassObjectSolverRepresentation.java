package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectInitializationConstraint;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

public interface PartnerClassObjectSolverRepresentation {

    static PartnerClassObjectSolverRepresentation newInstance(
            MulibConfig mc,
            PartnerClassObjectInitializationConstraint pc,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> symbolicArrayStates,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> symbolicPartnerClassObjectStates,
            int level) {
        PartnerClassObjectSolverRepresentation result;
        if (pc.getType() == PartnerClassObjectInitializationConstraint.Type.SIMPLE_PARTNER_CLASS_OBJECT) {
            result = new SimplePartnerClassObjectRepresentation(mc, pc, level);
        } else if (pc.getType() == PartnerClassObjectInitializationConstraint.Type.PARTNER_CLASS_OBJECT_IN_SARRAY) {
            throw new NotYetImplementedException();

//            Set<Sint> vals = aasr.getValuesKnownToPossiblyBeContainedInArray();
//            result = new AliasingPartnerClassObjectRepresentation(mc, pc, level, vals, );
        } else {
            assert pc.getType() == PartnerClassObjectInitializationConstraint.Type.ALIASED_PARTNER_CLASS_OBJECT;
            throw new NotYetImplementedException(); //// TODO
        }
        return result;
    }

    Constraint getField(String fieldName, Sprimitive value);

    void putField(String fieldName, Sprimitive value);

    Sint getId();
    Sbool isNull();
    int getLevel();
    Class<?> getClazz();

    boolean defaultIsSymbolic();

    PartnerClassObjectSolverRepresentation copyForNewLevel(int level);

}
