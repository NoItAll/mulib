package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.solving.Labels;
import de.wwu.mulib.substitutions.SubstitutedVar;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

public interface SolverManager {

    void addConstraintAfterNewBacktrackingPoint(Constraint constraint);

    void addConstraintsAfterNewBacktrackingPoint(List<Constraint> constraints);

    boolean checkWithNewConstraint(Constraint c);

    boolean isSatisfiable();

    void backtrackOnce();

    void backtrack(int numberOfChoiceOptions);

    void backtrackAll();

    Labels getLabels(Map<String, Object> primitivesToLabel);

    Object getLabel(SubstitutedVar var);

    ArrayDeque<Constraint> getConstraints();
}
