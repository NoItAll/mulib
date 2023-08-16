package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.Collection;

/**
 * Marker interface for array solver representations that are potentially symbolic aliases of other representations
 */
public interface AliasingArraySolverRepresentation extends ArraySolverRepresentation {
    /**
     * @return The metadata constraint
     */
    Constraint getMetadataConstraintForPotentialIds();

    /**
     * @return The identifiers of potentially symbolically aliased representations
     */
    Collection<Sint> getAliasedIds();

}
