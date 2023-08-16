package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectInitializationConstraint;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.Set;

/**
 * Interface for all representations of a non-array object FOR the constraint solver
 */
public interface PartnerClassObjectSolverRepresentation {

    /**
     * Constructs a new representation
     * Regards the various "initialization types" of the initialization constraint
     * @param config The configuration
     * @param pc The constraint initializing the non-array object
     * @param symbolicArrayStates The structure maintaining array representations for the solver
     * @param symbolicPartnerClassObjectStates The structure maintaining partner class objects
     * @param level The level at which we initialize the representation
     * @return The new representation for the solver
     * @see PartnerClassObjectInitializationConstraint.Type
     */
    static PartnerClassObjectSolverRepresentation newInstance(
            MulibConfig config,
            PartnerClassObjectInitializationConstraint pc,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> symbolicArrayStates,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> symbolicPartnerClassObjectStates,
            int level) {
        PartnerClassObjectSolverRepresentation result;
        if (pc.getType() == PartnerClassObjectInitializationConstraint.Type.SIMPLE_PARTNER_CLASS_OBJECT) {
            result = new SimplePartnerClassObjectSolverRepresentation(config, symbolicPartnerClassObjectStates, symbolicArrayStates, pc, level);
        } else if (pc.getType() == PartnerClassObjectInitializationConstraint.Type.PARTNER_CLASS_OBJECT_IN_SARRAY) {
            ArraySolverRepresentation asr =
                    symbolicArrayStates
                            .getRepresentationForId(pc.getContainingPartnerClassObjectId())
                            .getNewestRepresentation();
            assert asr instanceof PartnerClassArraySolverRepresentation;
            PartnerClassArraySolverRepresentation pasr = (PartnerClassArraySolverRepresentation) asr;
            Set<Sint> aliasedPcos = pasr.getValuesKnownToPossiblyBeContainedInArray();
            result = new AliasingPartnerClassObjectSolverRepresentation(config, symbolicPartnerClassObjectStates, symbolicArrayStates, pc, level, aliasedPcos, asr.isCompletelyInitialized());
        } else if (pc.getType() == PartnerClassObjectInitializationConstraint.Type.ALIASED_PARTNER_CLASS_OBJECT) {
            result = new AliasingPartnerClassObjectSolverRepresentation(config, symbolicPartnerClassObjectStates, symbolicArrayStates, pc, level, pc.getPotentialIds(), false);
        } else {
            assert pc.getType() == PartnerClassObjectInitializationConstraint.Type.PARTNER_CLASS_OBJECT_IN_PARTNER_CLASS_OBJECT;
            PartnerClassObjectSolverRepresentation psr =
                    symbolicPartnerClassObjectStates
                            .getRepresentationForId(pc.getContainingPartnerClassObjectId())
                            .getNewestRepresentation();
            assert psr != null;
            Set<Sint> ids = psr.getPartnerClassIdsKnownToBePossiblyContainedInField(pc.getFieldName(), true);
            result = new AliasingPartnerClassObjectSolverRepresentation(config, symbolicPartnerClassObjectStates, symbolicArrayStates, pc, level, ids, false); //// TODO is false sufficient or how to determine whether new instance or not?
        }
        return result;
    }

    /**
     * @param fieldName The name of the field to get the value from. Should follow the pattern: packageName.className.fieldName
     * @param value The value gotten from the field
     * @return A constraint signaling that when selecting from this object at the given field, then value is returned
     */
    Constraint getField(String fieldName, Sprimitive value);

    /**
     * Modifies the representation so that, subsequently, when getting the specified field, value should be returned
     * @param fieldName The field name of the field to set the value in. Should follow the pattern: packageName.className.fieldName
     * @param value The value stored into the field
     */
    void putField(String fieldName, Sprimitive value);

    /**
     * @param guard The guard
     * @param fieldName The name of the field to get the value from. Should follow the pattern: packageName.className.fieldName
     * @param value The value gotten from the field
     * @return A constraint signaling that when selecting from this object at the given field, then value is returned
     * IF guard evaluates to true
     */
    Constraint getField(Constraint guard, String fieldName, Sprimitive value);

    /**
     * Modifies the representation so that, subsequently, when getting the specified field, value should be returned IF
     * guard evaluates to true
     * @param fieldName The field name of the field to set the value in. Should follow the pattern: packageName.className.fieldName
     * @param value The value stored into the field
     */
    void putField(Constraint guard, String fieldName, Sprimitive value);

    /**
     * @return The identifier of the represented object
     */
    Sint getId();

    /**
     * @return Whether the represented object can be null
     */
    Sbool isNull();

    /**
     * @return The level the representation is for
     */
    int getLevel();

    /**
     * @return The class represented by this representation
     */
    Class<?> getClazz();

    /**
     * @return Whether the default value retrieved from fields is symbolic
     */
    boolean defaultIsSymbolic();

    /**
     * Copies the representation for a new level
     * @param level The new level
     * @return A new representation for the new level
     */
    PartnerClassObjectSolverRepresentation copyForNewLevel(int level);

    /**
     * @param fieldName The evaluated field's name. Should follow the pattern: packageName.className.fieldName.
     *                  The field must contain a reference-typed value
     * @param initializeSelfIfCanBeNew Whether this representation should be lazily initialized if it can represent a new
     *                                 instance
     * @return The set of identifiers representing objects potentially contained in this field
     */
    Set<Sint> getPartnerClassIdsKnownToBePossiblyContainedInField(String fieldName, boolean initializeSelfIfCanBeNew);

    /**
     * Lazily generates a field
     * @param field The field's name. Should follow the pattern: packageName.className.fieldName.
     * @return The representation with a lazily initialized field. The calling object is not necessarily returned as we
     * initialize a new representation if the current level does not equal to the representation's level.
     */
    PartnerClassObjectSolverRepresentation lazilyGenerateAndSetPartnerClassFieldIfNeeded(String field);

    /**
     * @param field The evaluated field's name. Should follow the pattern: packageName.className.fieldName.
     *              The field must contain a reference-typed value
     * @return true, if the field potentially contains null, else false
     */
    boolean partnerClassFieldCanPotentiallyContainNull(String field);

    /**
     * @param field The evaluated field's name. Should follow the pattern: packageName.className.fieldName.
     * @return true, if the field is initialized (potentially lazily), else false
     */
    boolean _fieldIsSet(String field);
}
