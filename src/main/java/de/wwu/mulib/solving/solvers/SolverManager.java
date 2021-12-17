package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.ArrayDeque;
import java.util.List;

public interface SolverManager {

    void addConstraint(Constraint constraint);

    void addConstraintAfterNewBacktrackingPoint(Constraint constraint);

    boolean checkWithNewConstraint(Constraint c);

    boolean isSatisfiable();

    void backtrackOnce();

    void backtrack(int numberOfChoiceOptions);

    void backtrackAll();

    Object getLabel(Sprimitive var);

    ArrayDeque<Constraint> getConstraints();

    int getLevel();
}
