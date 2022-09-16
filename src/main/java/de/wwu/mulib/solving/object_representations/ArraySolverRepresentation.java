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
            result = ac.getValueType().isArray() ?
                    new ArrayArraySolverRepresentation(ac, level)
                    :
                    new PrimitiveValuedArraySolverRepresentation(ac, level);
        } else if (ac.getType() == ArrayInitializationConstraint.Type.SARRAY_IN_SARRAY) {
            ArraySolverRepresentation asr =
                    symbolicArrayStates.getArraySolverRepresentationForId(ac.getContainingSarraySarrayId()).getNewestRepresentation();
            assert asr instanceof IArrayArraySolverRepresentation;
            IArrayArraySolverRepresentation aasr = (IArrayArraySolverRepresentation) asr;
            Set<Sint> aliasedArrays;
            if (asr.isCompletelyInitialized()) {
                aliasedArrays = aasr.getInitialConcreteAndStoredValues();
            } else {
                aliasedArrays = aasr.getPotentialValues();
            }
            result =
                    ac.getValueType().isArray() ?
                            new AliasingArrayArraySolverRepresentation(
                                    ac,
                                    level,
                                    aliasedArrays,
                                    symbolicArrayStates,
                                    asr.isCompletelyInitialized()
                            )
                            :
                            new AliasingPrimitiveValuedArraySolverRepresentation(
                                    ac,
                                    level,
                                    // The Sint-values here are the IDs of the aliased arrays
                                    aliasedArrays,
                                    symbolicArrayStates,
                                    asr.isCompletelyInitialized()
                            );
        } else {
            assert ac.getType() == ArrayInitializationConstraint.Type.ALIASED_SARRAY;
            result =
                    ac.getValueType().isArray() ?
                            new AliasingArrayArraySolverRepresentation(
                                    ac,
                                    level,
                                    ac.getPotentialIds(),
                                    symbolicArrayStates,
                                    false
                            )
                            :
                            new AliasingPrimitiveValuedArraySolverRepresentation(
                                    ac,
                                    level,
                                    ac.getPotentialIds(),
                                    symbolicArrayStates,
                                    false
                            );
        }
        return result;
    }

    Constraint select(Sint index, Sprimitive selectedValue);

    Constraint select(Constraint guard, Sint index, Sprimitive selectedValue);

    void store(Sint index, Sprimitive storedValue);

    void store(Constraint guard, Sint index, Sprimitive storedValue);

    ArraySolverRepresentation copyForNewLevel(int level);

    Sint getArrayId();

    Sint getLength();

    Sbool getIsNull();

    int getLevel();

    boolean isCompletelyInitialized();

    Class<?> getElementType();

}
