package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.Constraint;

public interface AliasingArraySolverRepresentation extends ArraySolverRepresentation {
    Constraint getMetadataConstraintForPotentialIds();

}
