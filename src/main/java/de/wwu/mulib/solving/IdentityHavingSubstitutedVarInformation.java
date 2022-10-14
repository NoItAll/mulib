package de.wwu.mulib.solving;

import de.wwu.mulib.solving.object_representations.ArraySolverRepresentation;
import de.wwu.mulib.solving.object_representations.IArrayArraySolverRepresentation;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.Sbool;

public class IdentityHavingSubstitutedVarInformation {

    public final Sbool isNull;
    public final boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault;
    public final boolean canContainExplicitNull;

    public IdentityHavingSubstitutedVarInformation(
            IncrementalSolverState.SymbolicArrayStates<ArraySolverRepresentation> sas,
            ArraySolverRepresentation asr) {
        // z.B. RangeExpression-Objekt: private int lowerBound; privae int upperBound;
        this.isNull = asr.getIsNull();
        this.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault =
                asr.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault();

        this.canContainExplicitNull =
                asr instanceof IArrayArraySolverRepresentation
                        && ((IArrayArraySolverRepresentation) asr).canContainNull(sas);
    }

}
