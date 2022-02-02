package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.constraints.ArrayConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.ArrayDeque;
import java.util.List;

public interface SolverManager {

    void addConstraint(Constraint constraint);

    void addConstraintAfterNewBacktrackingPoint(Constraint constraint);

    void addArrayConstraint(ArrayConstraint ac);

    void addArrayConstraints(List<ArrayConstraint> acs);

    void addTemporaryAssumption(Constraint c);

    void resetTemporaryAssumptions();

    List<Constraint> getTemporaryAssumptions();

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
}
