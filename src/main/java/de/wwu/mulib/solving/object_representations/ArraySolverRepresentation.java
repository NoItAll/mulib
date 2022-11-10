package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.Set;

public interface ArraySolverRepresentation {

    static ArraySolverRepresentation newInstance(
            MulibConfig config,
            ArrayInitializationConstraint ac,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> symbolicArrayStates,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> symbolicPartnerClassObjectStates,
            int level) {
        boolean isPrimitive = Sprimitive.class.isAssignableFrom(ac.getValueType());
        boolean isArray = ac.getValueType().isArray();
        boolean isPartnerClass = PartnerClass.class.isAssignableFrom(ac.getValueType());
        assert isPrimitive || isArray || isPartnerClass;
        ArraySolverRepresentation result;
        if (ac.getType() == ArrayInitializationConstraint.Type.SIMPLE_SARRAY) {
            result = isArray || isPartnerClass ?
                    new SimplePartnerClassArraySolverRepresentation(
                            config,
                            ac,
                            symbolicPartnerClassObjectStates,
                            symbolicArrayStates,
                            level
                    )
                    :
                    new PrimitiveValuedArraySolverRepresentation(config, ac, level);
        } else if (ac.getType() == ArrayInitializationConstraint.Type.SARRAY_IN_SARRAY) {
            ArraySolverRepresentation asr =
                    symbolicArrayStates
                            .getRepresentationForId(ac.getContainingPartnerClassObjectId())
                            .getNewestRepresentation();
            assert asr instanceof PartnerClassArraySolverRepresentation;
            PartnerClassArraySolverRepresentation aasr = (PartnerClassArraySolverRepresentation) asr;
            Set<Sint> aliasedArrays = aasr.getValuesKnownToPossiblyBeContainedInArray();
            result =
                    isArray || isPartnerClass ?
                            new AliasingPartnerClassArraySolverRepresentation(
                                    config,
                                    ac,
                                    level,
                                    aliasedArrays,
                                    symbolicPartnerClassObjectStates,
                                    symbolicArrayStates,
                                    asr.isCompletelyInitialized()
                            )
                            :
                            new AliasingPrimitiveValuedArraySolverRepresentation(
                                    config,
                                    ac,
                                    level,
                                    // The Sint-values here are the IDs of the aliased arrays
                                    aliasedArrays,
                                    symbolicArrayStates,
                                    asr.isCompletelyInitialized()
                            );
        } else if (ac.getType() == ArrayInitializationConstraint.Type.ALIASED_SARRAY) {
            result =
                    isArray || isPartnerClass ?
                            new AliasingPartnerClassArraySolverRepresentation(
                                    config,
                                    ac,
                                    level,
                                    ac.getPotentialIds(),
                                    symbolicPartnerClassObjectStates,
                                    symbolicArrayStates,
                                    false
                            )
                            :
                            new AliasingPrimitiveValuedArraySolverRepresentation(
                                    config,
                                    ac,
                                    level,
                                    ac.getPotentialIds(),
                                    symbolicArrayStates,
                                    false
                            );
        } else {
            assert ac.getType() == ArrayInitializationConstraint.Type.SARRAY_IN_PARTNER_CLASS_OBJECT;
            PartnerClassObjectSolverRepresentation psr =
                    symbolicPartnerClassObjectStates
                            .getRepresentationForId(ac.getContainingPartnerClassObjectId())
                            .getNewestRepresentation();
            Set<Sint> ids = psr.getPartnerClassIdsKnownToBePossiblyContainedInField(ac.getFieldName());
            result = isArray || isPartnerClass ?
                    new AliasingPartnerClassArraySolverRepresentation(
                            config,
                            ac,
                            level,
                            ids,
                            symbolicPartnerClassObjectStates,
                            symbolicArrayStates,
                            true
                    )
                    :
                    new AliasingPrimitiveValuedArraySolverRepresentation(config, ac, level, ids, symbolicArrayStates, true);
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

    boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault();

    Class<?> getElementType();

    boolean defaultIsSymbolic();

    ArrayHistorySolverRepresentation getCurrentRepresentation();

}
