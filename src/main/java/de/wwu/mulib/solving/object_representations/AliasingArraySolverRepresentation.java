package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.Collection;

public interface AliasingArraySolverRepresentation extends ArraySolverRepresentation {
    Constraint getMetadataConstraintForPotentialIds();

    Collection<Sint> getAliasedIds();

}
