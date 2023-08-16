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

/**
 * Interface for all representations of a Sarray FOR the constraint solver
 */
public interface ArraySolverRepresentation {

    /**
     * Constructs a new representation
     * Regards the various "initialization types" of the initialization constraint
     * @param config The configuration
     * @param ac The constraint initializing the array
     * @param symbolicArrayStates The structure maintaining symbolic arrays
     * @param symbolicPartnerClassObjectStates The structure maintaining partner class objects
     * @param level The level at which we initialize the representation
     * @return The new representation for the solver
     * @see ArrayInitializationConstraint.Type
     */
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
//            assert !aliasedArrays.isEmpty();
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
            Set<Sint> ids = psr.getPartnerClassIdsKnownToBePossiblyContainedInField(ac.getFieldName(), true);
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

    /**
     * @param index The index
     * @param selectedValue The selected value
     * @return A constraint signaling that when selecting from the represented array at the given index,
     * the selectedValue is returned
     */
    Constraint select(Sint index, Sprimitive selectedValue);

    /**
     * @param guard The guard
     * @param index The index
     * @param selectedValue The selected value
     * @return A constraint signaling that when selecting from the represented array at the given index,
     * the selectedValue is returned IF the guard parameter evaluates to true
     */
    Constraint select(Constraint guard, Sint index, Sprimitive selectedValue);

    /**
     * Modifies this representation so reads at the specified index subsequently should return the specified value
     * @param index The index
     * @param storedValue The value
     */
    void store(Sint index, Sprimitive storedValue);

    /**
     * Modifies this representation so reads at the specified index subsequently should return the specified value
     * IF guard evaluates to true
     * @param guard The guard
     * @param index The index
     * @param storedValue The value
     */
    void store(Constraint guard, Sint index, Sprimitive storedValue);

    /**
     * Copies the representation for a new level
     * @param level The new level
     * @return A new representation for the new level
     */
    ArraySolverRepresentation copyForNewLevel(int level);

    /**
     * @return The identifier of the representation
     */
    Sint getArrayId();

    /**
     * @return The length of the representation
     */
    Sint getLength();

    /**
     * @return Whether the representation is null
     */
    Sbool getIsNull();

    /**
     * @return The level of the representation
     */
    int getLevel();

    /**
     * @return true, if the representation is completely initialized, false, if there are yet unknown elements
     */
    boolean isCompletelyInitialized();

    /**
     * If this is true, {@link ArraySolverRepresentation#isCompletelyInitialized()} must be false
     * @return true, if unknown elements might contain Java's default values (e.g. 0 for int, in Mulib -1 for null) etc.
     */
    boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault();

    /**
     * @return The type of elements
     */
    Class<?> getElementType();

    /**
     * @return Whether the default element of the represented array is symbolic
     */
    boolean defaultIsSymbolic();
}
