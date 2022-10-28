package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.PartnerClassObjectInitializationConstraint;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;

public interface PartnerClassObjectSolverRepresentation {

    static PartnerClassObjectSolverRepresentation newInstance(
            MulibConfig mc,
            PartnerClassObjectInitializationConstraint pc,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> symbolicPartnerClassObjectStates,
            int level) {
        throw new NotYetImplementedException(); //// TODO
    }

}
