package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.constraints.ArrayConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.IdentityHavingSubstitutedVarConstraint;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.solving.IdentityHavingSubstitutedVarInformation;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public interface SolverManager {

    void addConstraint(Constraint constraint);

    void addConstraintAfterNewBacktrackingPoint(Constraint constraint);

    void addIdentityHavingSubstitutedVarConstraint(IdentityHavingSubstitutedVarConstraint ac);

    void addIdentityHavingSubstitutedVarConstraints(List<IdentityHavingSubstitutedVarConstraint> acs);

    boolean checkWithNewConstraint(Constraint c);

    boolean isSatisfiable();

    void backtrackOnce();

    void backtrack(int numberOfChoiceOptions);

    void backtrackAll();

    void resetLabels();

    void registerLabelPair(Object searchRegionRepresentation, Object labeled);

    /**
     * Transforms the search space-representation of an object into the original representation useable by a usual
     * Java program.
     * @param var The to-be-transformed Object. The type 'SubstitutedVar' is not used here, since, e.g., ArrayLists might also
     *            be labeled with a custom labeling strategy.
     * @return The labeled object.
     */
    Object getLabel(Object var);

    ArrayDeque<Constraint> getConstraints();

    List<ArrayConstraint> getArrayConstraints();

    int getLevel();

    List<Solution> getUpToNSolutions(Solution initialSolution, AtomicInteger N);

    IdentityHavingSubstitutedVarInformation getAvailableInformationOnIdentityHavingSubstitutedVar(Sint id);
}
