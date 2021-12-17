package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.And;
import de.wwu.mulib.constraints.Constraint;

import java.util.ArrayDeque;
import java.util.List;

public abstract class AbstractIncrementalEnabledSolverManager<M> implements SolverManager {
    protected final ArrayDeque<Constraint> constraints = new ArrayDeque<>();
    protected final boolean labelSymbolicValues;

    protected M currentModel;
    private int level = 0;
    private boolean isSatisfiable;
    private boolean satisfiabilityWasCalculated;

    protected AbstractIncrementalEnabledSolverManager(MulibConfig config) {
        this.labelSymbolicValues = config.LABEL_SYMBOLIC_VALUES;
    }

    protected abstract M calculateCurrentModel();

    protected abstract void addSolverConstraintRepresentation(Constraint constraint);

    protected abstract boolean calculateIsSatisfiable();

    @Override
    public ArrayDeque<Constraint> getConstraints() {
        return new ArrayDeque<>(constraints); // Wrap and return
    }

    @Override
    public boolean isSatisfiable() {
        assert level != 0: "The initial dummy choice should always be present";
        if (!satisfiabilityWasCalculated) {
            isSatisfiable = calculateIsSatisfiable();
            satisfiabilityWasCalculated = true;
        }
        return isSatisfiable;
    }

    protected abstract void solverSpecificBacktrackingPoint();

    protected final M getCurrentModel() {
        if (currentModel == null) {
            currentModel = calculateCurrentModel();
        }
        return currentModel;
    }

    @Override
    public void addConstraintsAfterNewBacktrackingPoint(List<Constraint> constraints) {
        solverSpecificBacktrackingPoint();
        level++;
        _addConstraint(And.newInstance(constraints));
    }

    @Override
    public void addConstraintAfterNewBacktrackingPoint(Constraint c) {
        solverSpecificBacktrackingPoint();
        level++;
        _addConstraint(c);
    }

    protected void _addConstraint(Constraint constraint) {
        addSolverConstraintRepresentation(constraint);
        constraints.push(constraint);
        satisfiabilityWasCalculated = false;
        currentModel = null;
    }

    protected abstract void solverSpecificBacktrackOnce();

    protected abstract void solverSpecificBacktrack(int toBacktrack);

    @Override
    public final void backtrackOnce() {
        solverSpecificBacktrackOnce();
        constraints.pop();
        level--;
        satisfiabilityWasCalculated = false;
        currentModel = null;
    }

    @Override
    public final void backtrack(int numberOfChoiceOptions) {
        solverSpecificBacktrack(numberOfChoiceOptions);
        for (int i = 0; i < numberOfChoiceOptions; i++) {
            constraints.pop();
        }
        level -= numberOfChoiceOptions;
        satisfiabilityWasCalculated = false;

        currentModel = null;
    }

    @Override
    public final void backtrackAll() {
        backtrack(level);
    }
}
