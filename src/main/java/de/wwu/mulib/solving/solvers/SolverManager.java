package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.constraints.ArrayConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.substitutions.primitives.Sprimitive;
import de.wwu.mulib.transformations.MulibValueLabeler;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public interface SolverManager {

    void addConstraint(Constraint constraint);

    void addConstraintAfterNewBacktrackingPoint(Constraint constraint);

    void addArrayConstraint(ArrayConstraint ac);

    void addArrayConstraints(List<ArrayConstraint> acs);

    boolean checkWithNewArraySelectConstraint(ArrayConstraint ac);

    boolean checkWithNewConstraint(Constraint c);

    boolean isSatisfiable();

    void backtrackOnce();

    void backtrack(int numberOfChoiceOptions);

    void backtrackAll();

    Object getLabel(Sprimitive var);

    ArrayDeque<Constraint> getConstraints();

    List<ArrayConstraint> getArrayConstraints();

    int getLevel();

    List<Solution> getUpToNSolutions(Solution initialSolution, AtomicInteger N, MulibValueLabeler mulibValueLabeler);
}
