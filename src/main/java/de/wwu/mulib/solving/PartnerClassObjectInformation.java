package de.wwu.mulib.solving;

import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.solving.object_representations.PartnerClassObjectSolverRepresentation;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.Sbool;

public class PartnerClassObjectInformation {

    public final String forField;
    public final Sbool isNull;
    public final boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault;
    public final boolean canContainExplicitNull;

    public PartnerClassObjectInformation(
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sas,
            PartnerClassObjectSolverRepresentation asr,
            String field) {
        //// TODO
        throw new NotYetImplementedException();
    }

}
