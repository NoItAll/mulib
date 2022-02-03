package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.And;
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
 * @param <M> Class representing a solver's model from which value assignments can be derived
 * @param <B> Class representing constraints in the solver
 * @param <AR> Class representing array expressions in the solver
 */
public abstract class AbstractIncrementalEnabledSolverManager<M, B, AR> implements SolverManager {
    private final IncrementalSolverState<AR> incrementalSolverState;
    private M currentModel;
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

    // For internal use without conservative copy
    protected ArrayDeque<Constraint> _getConstraints() {
        return incrementalSolverState.getConstraints();
    }

    @Override
    public final boolean checkWithNewConstraint(Constraint c) {
        if (c instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) c).isTrue();
        }
        B bool = transformConstraint(c);
        return _check(bool);
    }

    @Override
    public final boolean checkWithNewArraySelectConstraint(ArrayConstraint ac) {
        B bool = newArraySelectConstraint(incrementalSolverState.getCurrentArrayRepresentation(ac.getArrayId()), ac.getIndex(), ac.getValue());
        return _check(bool);
    }

    private boolean _check(B bool) {
        boolean result = calculateSatisfiabilityWithSolverBoolRepresentation(bool);
        if (!result) {
            // For instance Z3 bases its solver.getModel() on the last sat-check. Hence
            // if this is unsatisfiable, we should reset the model.
            resetSatisfiabilityWasCalculatedAndModel();
        }
        return result;
    }

    @Override
    public final List<ArrayConstraint> getArrayConstraints() {
        return incrementalSolverState.getArrayConstraints();
    }

    @Override
    public final boolean isSatisfiable() {
        assert incrementalSolverState.getLevel() != 0: "The initial choice should always be present";
        if (!satisfiabilityWasCalculated) {
            isSatisfiable = incrementalSolverState.getTemporaryAssumptions().size() > 0 ?
                    calculateSatisfiabilityWithSolverBoolRepresentation(transformConstraint(And.newInstance(incrementalSolverState.getTemporaryAssumptions())))
                    :
                    calculateIsSatisfiable();
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
        resetSatisfiabilityWasCalculatedAndModel();
    }

    protected final M getCurrentModel() {
        if (currentModel == null) {
            try {
                currentModel = calculateCurrentModel();
            } catch (Throwable t) {
                t.printStackTrace();
                throw new MulibRuntimeException(t);
            }
        }
        return currentModel;
    }

    @Override
    public final void addConstraint(Constraint c) {
        incrementalSolverState.addConstraint(c);
        resetSatisfiabilityWasCalculatedAndModel();
        try {
            addSolverConstraintRepresentation(transformConstraint(c));
        } catch (Throwable t) {
            t.printStackTrace();
            throw new MulibRuntimeException(t);
        }
    }

    @Override
    public final void addConstraintAfterNewBacktrackingPoint(Constraint c) {
        resetSatisfiabilityWasCalculatedAndModel();
        try {
            incrementalSolverState.pushConstraint(c);
            solverSpecificBacktrackingPoint();
            addSolverConstraintRepresentation(transformConstraint(c));
        } catch (Throwable t) {
            t.printStackTrace();
            throw new MulibRuntimeException(t);
        }
    }

    @Override
    public void addTemporaryAssumption(Constraint c) {
        resetSatisfiabilityWasCalculatedAndModel();
        incrementalSolverState.addTemporaryAssumption(c);
    }

    @Override
    public void resetTemporaryAssumptions() {
        if (!incrementalSolverState.getTemporaryAssumptions().isEmpty()) {
            resetSatisfiabilityWasCalculatedAndModel();
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
        resetSatisfiabilityWasCalculatedAndModel();
    }

    @Override
    public final void backtrack(int numberOfChoiceOptions) {
        solverSpecificBacktrack(numberOfChoiceOptions);
        for (int i = 0; i < numberOfChoiceOptions; i++) {
            incrementalSolverState.popConstraint();
        }
        if (numberOfChoiceOptions > 0) {
            resetSatisfiabilityWasCalculatedAndModel();
        }
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

    protected abstract void addSolverConstraintRepresentation(B constraint);

    protected abstract boolean calculateIsSatisfiable();

    protected abstract AR createCompletelyNewArrayRepresentation(ArrayConstraint ac);

    protected abstract AR createNewArrayRepresentationForStore(ArrayConstraint ac, AR oldRepresentation);

    protected abstract void addArraySelectConstraint(AR arrayRepresentation, Sint index, SubstitutedVar value);

    protected abstract void solverSpecificBacktrackingPoint();

    protected abstract void solverSpecificBacktrackOnce();

    protected abstract void solverSpecificBacktrack(int toBacktrack);

    protected abstract boolean calculateSatisfiabilityWithSolverBoolRepresentation(B boolExpr);

    protected abstract B newArraySelectConstraint(AR arrayRepresentation, Sint indexInArray, SubstitutedVar arrayValue);

    protected abstract B transformConstraint(Constraint c);

    private void resetSatisfiabilityWasCalculatedAndModel() {
        satisfiabilityWasCalculated = false;
        currentModel = null;
    }
}
