package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.Set;

public interface ArraySolverRepresentation {

    static ArraySolverRepresentation newInstance(
            ArrayInitializationConstraint ac,
            IncrementalSolverState.SymbolicArrayStates<ArraySolverRepresentation> symbolicArrayStates,
            int level) {
        ArraySolverRepresentation result;
        if (ac.getType() == ArrayInitializationConstraint.Type.SIMPLE_SARRAY) {
            result = new PrimitiveValuedArraySolverRepresentation(
                    ac.getArrayId(),
                    ac.getArrayLength(),
                    ac.getIsNull(),
                    level
            );
        } else if (ac.getType() == ArrayInitializationConstraint.Type.SARRAY_IN_SARRAY) {
            ArraySolverRepresentation asr =
                    symbolicArrayStates.getArraySolverRepresentationForId(ac.getContainingSarraySarrayId()).getNewestRepresentation();
            result = new AliasingArraySolverRepresentation(
                    ac,
                    level,
                    // The Sint-values here are the IDs of the aliased arrays
                    (Set<Sint>) asr.getPotentialValues(),
                    symbolicArrayStates
            );
        } else {
            assert ac.getType() == ArrayInitializationConstraint.Type.ALIASED_SARRAY;
            result = new AliasingArraySolverRepresentation(
                    ac,
                    level,
                    ac.getPotentialIds(),
                    symbolicArrayStates
            );
        }
        return result;
    }

    default Constraint select(Sint index, Sprimitive selectedValue) {
        return select(Sbool.ConcSbool.TRUE, index, selectedValue);
    }

    Constraint select(Constraint guard, Sint index, Sprimitive selectedValue);

    default void store(Sint index, Sprimitive storedValue) {
        store(Sbool.ConcSbool.TRUE, index, storedValue);
    }

    void store(Constraint guard, Sint index, Sprimitive storedValue);

    ArraySolverRepresentation copyForNewLevel(int level);

    Sint getArrayId();

    Sint getLength();

    Sbool getIsNull();

    int getLevel();

    Set<? extends Sprimitive> getPotentialValues();

}
