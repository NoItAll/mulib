package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.Set;

public interface IArrayArraySolverRepresentation extends ArraySolverRepresentation {

    Set<Sint> getValuesKnownToPossiblyBeContainedInArray();

    @Override
    IArrayArraySolverRepresentation copyForNewLevel(int level);

    default boolean canContainNull(IncrementalSolverState.SymbolicArrayStates<ArraySolverRepresentation> sas) {
        Set<Sint> relevantValues = getValuesKnownToPossiblyBeContainedInArray();
        if (relevantValues.stream().anyMatch(s -> s == Sint.ConcSint.MINUS_ONE)) {
            return true;
        }
        return relevantValues.stream()
                .anyMatch(s -> {
                    ArraySolverRepresentation asr = sas.getArraySolverRepresentationForId(s).getNewestRepresentation();
                    return asr.getIsNull() instanceof Sym;
                });
    }
}
