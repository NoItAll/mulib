package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;

/**
 *
 * @param <M> Class representing a Model from which value assignments can be derived
 * @param <AR> Class representing an array
 */
public abstract class AbstractIncrementalEnabledSolverManager<M, B, AR> implements SolverManager {
    protected final IncrementalSolverState<AR> incrementalSolverState;

    protected M currentModel;
    private boolean isSatisfiable;
    private boolean satisfiabilityWasCalculated;
    protected final MulibConfig config;

    protected AbstractIncrementalEnabledSolverManager(MulibConfig config) {
        this.config = config;
        this.incrementalSolverState = IncrementalSolverState.newInstance(config);
    }

    @Override
    public final ArrayDeque<Constraint> getConstraints() {
        return new ArrayDeque<>(incrementalSolverState.getConstraints()); // Wrap and return
    }

    @Override
    public final List<ArrayConstraint> getArrayConstraints() {
        return incrementalSolverState.getArrayConstraints();
    }

    @Override
    public final boolean isSatisfiable() {
        assert incrementalSolverState.getLevel() != 0: "The initial choice should always be present";
        if (!satisfiabilityWasCalculated) {
            isSatisfiable = calculateIsSatisfiable();
            satisfiabilityWasCalculated = true;
        }
        return isSatisfiable;
    }

    @Override
    public final void addArrayConstraints(List<ArrayConstraint> acs) {
        for (ArrayConstraint ac : acs) {
            addArrayConstraint(ac);
        }
    }

    // Treatment of free arrays is inspired by that of Muli, yet modified. E.g., the ArrayConstraint is not a subtype of Constraint in Mulib:
    // https://github.com/wwu-pi/muggl/blob/53a2874cba2b193ec99d2aea8a454a88481656c7/muggl-solver-z3/src/main/java/de/wwu/muggl/solvers/z3/Z3MugglAdapter.java
    @Override
    public final void addArrayConstraint(ArrayConstraint ac) {
        incrementalSolverState.addArrayConstraintAtLevel(ac);
        AR arrayRepresentation = incrementalSolverState.getCurrentArrayRepresentation(ac.getArrayId());
        if (ac.getType() == ArrayConstraint.Type.SELECT) {
            if (arrayRepresentation == null) {
                arrayRepresentation = createCompletelyNewArrayRepresentation(ac);
                incrementalSolverState.addRepresentationInitializingArrayConstraint(ac, arrayRepresentation);
            }
            addArraySelectConstraint(arrayRepresentation, ac.getIndex(), ac.getValue());
        } else {
            if (arrayRepresentation == null) {
                arrayRepresentation = createCompletelyNewArrayRepresentation(ac);
            } else {
                arrayRepresentation = createNewArrayRepresentationForStore(ac, arrayRepresentation);
            }
            incrementalSolverState.addRepresentationInitializingArrayConstraint(ac, arrayRepresentation);
        }
        assert calculateIsSatisfiable();
    }

    protected final M getCurrentModel() {
        if (currentModel == null) {
            currentModel = calculateCurrentModel();
        }
        return currentModel;
    }

    @Override
    public final void addConstraint(Constraint c) {
        incrementalSolverState.addConstraint(c);
        satisfiabilityWasCalculated = false;
        currentModel = null;
        try {
            addSolverConstraintRepresentation(c);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new MulibRuntimeException(t);
        }
    }

    @Override
    public final void addConstraintAfterNewBacktrackingPoint(Constraint c) {
        if (c != Sbool.TRUE) {
            satisfiabilityWasCalculated = false;
            currentModel = null;
        }
        try {
            incrementalSolverState.pushConstraint(c);
            solverSpecificBacktrackingPoint();
            addSolverConstraintRepresentation(c);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new MulibRuntimeException(t);
        }
    }

    @Override
    public void addTemporaryAssumption(Constraint c) {
        satisfiabilityWasCalculated = false;
        currentModel = null;
        incrementalSolverState.addTemporaryAssumption(c);
    }

    @Override
    public void resetTemporaryAssumptions() {
        if (!incrementalSolverState.getTemporaryAssumptions().isEmpty()) {
            satisfiabilityWasCalculated = false;
            currentModel = null;
            incrementalSolverState.resetTemporaryAssumptions();
        }
    }

    @Override
    public List<Constraint> getTemporaryAssumptions() {
        return Collections.unmodifiableList(incrementalSolverState.getTemporaryAssumptions());
    }

    @Override
    public final void backtrackOnce() {
        solverSpecificBacktrackOnce();
        incrementalSolverState.popConstraint();
        satisfiabilityWasCalculated = false;
        currentModel = null;
    }

    @Override
    public final void backtrack(int numberOfChoiceOptions) {
        solverSpecificBacktrack(numberOfChoiceOptions);
        for (int i = 0; i < numberOfChoiceOptions; i++) {
            incrementalSolverState.popConstraint();
        }
        satisfiabilityWasCalculated = false;

        currentModel = null;
    }

    @Override
    public final void backtrackAll() {
        backtrack(incrementalSolverState.getLevel());
    }

    @Override
    public final int getLevel() {
        return incrementalSolverState.getLevel();
    }

    protected abstract M calculateCurrentModel();

    protected abstract void addSolverConstraintRepresentation(Constraint constraint);

    protected abstract boolean calculateIsSatisfiable();

    protected abstract AR createCompletelyNewArrayRepresentation(ArrayConstraint ac);

    protected abstract AR createNewArrayRepresentationForStore(ArrayConstraint ac, AR oldRepresentation);

    protected abstract void addArraySelectConstraint(AR arrayRepresentation, Sint index, SubstitutedVar value);

    protected abstract void solverSpecificBacktrackingPoint();

    protected abstract void solverSpecificBacktrackOnce();

    protected abstract void solverSpecificBacktrack(int toBacktrack);
}
