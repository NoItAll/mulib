package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.Set;

public interface PartnerClassArraySolverRepresentation extends ArraySolverRepresentation {

    Set<Sint> getValuesKnownToPossiblyBeContainedInArray();

    @Override
    PartnerClassArraySolverRepresentation copyForNewLevel(int level);

    default boolean canContainNull(IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> sas) {
        Set<Sint> relevantValues = getValuesKnownToPossiblyBeContainedInArray();
        return relevantValues.stream()
                .anyMatch(s -> {
                    if (s == Sint.ConcSint.MINUS_ONE) {
                        return true;
                    }
                    ArraySolverRepresentation asr = sas.getRepresentationForId(s).getNewestRepresentation();
                    return asr.getIsNull() instanceof Sym;
                });
    }
}
