package de.wwu.mulib.solving;

import de.wwu.mulib.solving.object_representations.ArraySolverRepresentation;
import de.wwu.mulib.solving.object_representations.PartnerClassArraySolverRepresentation;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.Sbool;

/**
 * Contains metadata for an array that is represented for/int the solver
 */
public class ArrayInformation {
    /**
     * Can the array be null?
     */
    public final Sbool isNull;
    /**
     * Can the array potentially contain unrepresented non symbolic default values?
     */
    public final boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault;
    /**
     * Are there potentially null values in the array?
     */
    public final boolean arrayCanPotentiallyContainNull;

    /**
     * @param sas The structure maintaining the representations for/in the constraint solver
     * @param asr The array solver representation
     */
    public ArrayInformation(
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> sas,
            ArraySolverRepresentation asr) {
        this.isNull = asr.getIsNull();
        this.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault =
                asr.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault();

        this.arrayCanPotentiallyContainNull =
                asr instanceof PartnerClassArraySolverRepresentation
                        && ((PartnerClassArraySolverRepresentation) asr).canContainNull(sas);
    }
}
