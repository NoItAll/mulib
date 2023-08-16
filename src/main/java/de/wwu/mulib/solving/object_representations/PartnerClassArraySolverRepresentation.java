package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.Set;

/**
 * Interface for all representations of an array of reference-typed elements FOR the constraint solver
 */
public interface PartnerClassArraySolverRepresentation extends ArraySolverRepresentation {

    /**
     * @return The identifiers of the objects that are stored in the sarray
     */
    Set<Sint> getValuesKnownToPossiblyBeContainedInArray();

    @Override
    PartnerClassArraySolverRepresentation copyForNewLevel(int level);

    /**
     * @param sas The structure maintaining the state of representations of arrays and objects
     * @return True if one of the elements potentially is null
     */
    @SuppressWarnings("rawuse")
    default boolean canContainNull(IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> sas) {
        Set<Sint> relevantValues = getValuesKnownToPossiblyBeContainedInArray();
        return relevantValues.stream()
                .anyMatch(s -> {
                    if (s == Sint.ConcSint.MINUS_ONE) {
                        return true;
                    }
                    Class<?> type = getElementType();
                    if (!type.isArray()) {
                        PartnerClassObjectSolverRepresentation partnerClassObjectConstraint =
                                (PartnerClassObjectSolverRepresentation) sas.getRepresentationForId(s).getNewestRepresentation();
                        return partnerClassObjectConstraint.isNull() instanceof Sym;
                    } else {
                        ArraySolverRepresentation asr = sas.getRepresentationForId(s).getNewestRepresentation();
                        return asr.getIsNull() instanceof Sym;
                    }
                });
    }
}
