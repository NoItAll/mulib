package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.And;
import de.wwu.mulib.constraints.Constraint;

import java.util.ArrayDeque;

public abstract class AbstractIncrementalEnabledSolverManager<M> implements SolverManager {
    // Each constraint represents one "scope" of a constraint here. That means tha a pop in a managed constraint solver
    // corresponds to a pop here.
    protected final ArrayDeque<Constraint> constraints = new ArrayDeque<>();

    protected M currentModel;
    private int level = 0;
    private boolean isSatisfiable;
    private boolean satisfiabilityWasCalculated;
    protected final MulibConfig config;

    protected AbstractIncrementalEnabledSolverManager(MulibConfig config) {
        this.config = config;
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
    public void addConstraint(Constraint c) {
        addSolverConstraintRepresentation(c);
        // We conjoin the previous with the current constraint so that the uppermost constraint is still a valid
        // representation of the current constraint scope
        Constraint previousTop = constraints.pollFirst();
        constraints.push(And.newInstance(previousTop, c));
        satisfiabilityWasCalculated = false;
        currentModel = null;
    }

    @Override
    public void addConstraintAfterNewBacktrackingPoint(Constraint c) {
        solverSpecificBacktrackingPoint();
        level++;
        addSolverConstraintRepresentation(c);
        constraints.push(c);
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

    public final int getLevel() {
        return level;
    }
}
