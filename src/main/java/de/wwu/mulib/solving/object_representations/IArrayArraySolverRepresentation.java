package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.Set;

public interface IArrayArraySolverRepresentation extends ArraySolverRepresentation {

    Set<Sint> getPotentialValues();

    Set<Sint> getInitialConcreteAndStoredValues();

    @Override
    IArrayArraySolverRepresentation copyForNewLevel(int level);


}
