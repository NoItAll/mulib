package de.wwu.mulib.solving;

import de.wwu.mulib.solving.object_representations.ArraySolverRepresentation;
import de.wwu.mulib.solving.object_representations.IArrayArraySolverRepresentation;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.Sbool;

public class ArrayInformation {
    public final Sbool isNull;
    public final boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault;
    public final boolean canContainExplicitNull;

    public ArrayInformation(
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> sas,
            ArraySolverRepresentation asr) {
        this.isNull = asr.getIsNull();
        this.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault =
                asr.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault();

        this.canContainExplicitNull =
                asr instanceof IArrayArraySolverRepresentation
                        && ((IArrayArraySolverRepresentation) asr).canContainNull(sas);
    }
}
