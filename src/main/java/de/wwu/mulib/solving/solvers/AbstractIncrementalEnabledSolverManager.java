package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.solving.LabelUtility;
import de.wwu.mulib.solving.Labels;
import de.wwu.mulib.substitutions.Conc;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.MulibValueLabeler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

    @SuppressWarnings("unchecked")
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
        resetSatisfiabilityWasCalculatedAndModel();
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
        incrementalSolverState.addArrayConstraint(ac);
        AR arrayRepresentation = incrementalSolverState.getCurrentArrayRepresentation(ac.getArrayId());
        if (ac.getType() == ArrayConstraint.Type.SELECT) {
            if (arrayRepresentation == null) {
                arrayRepresentation = createCompletelyNewArrayRepresentation(ac);
                incrementalSolverState.addRepresentationInitializingArrayConstraint(ac, arrayRepresentation);
            }
            addArraySelectConstraint(arrayRepresentation, ac.getIndex(), ac.getValue());
        } else {
            assert ac.getType() == ArrayConstraint.Type.STORE;
            if (arrayRepresentation == null) {
                arrayRepresentation = createCompletelyNewArrayRepresentation(ac);
            }
            arrayRepresentation = createNewArrayRepresentationForStore(ac, arrayRepresentation);
            incrementalSolverState.addRepresentationInitializingArrayConstraint(ac, arrayRepresentation);
        }
        resetSatisfiabilityWasCalculatedAndModel();
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

    @Override
    public List<Solution> getUpToNSolutions(final Solution initialSolution, AtomicInteger N, MulibValueLabeler mulibValueLabeler) {
        Solution latestSolution = initialSolution;
        if (latestSolution.labels.getNamedVars().length == 0) {
            return Collections.singletonList(initialSolution); // No named variables --> nothing to negate.
        }
        List<Solution> solutions = new ArrayList<>();
        int backtrackAfter = 0;
        int currentN = N.get();
        while (currentN > 0) {
            Labels l = latestSolution.labels;

            SubstitutedVar[] namedVars = l.getNamedVars();
            List<Constraint> disjunctionConstraints = new ArrayList<>();
            for (SubstitutedVar sv : namedVars) {
                if (sv instanceof Sprimitive) {
                    Constraint disjunctionConstraint = getNeq(sv, l.getLabelForNamedSubstitutedVar(sv));
                    disjunctionConstraints.add(disjunctionConstraint);
                }
            }

            Constraint newConstraint = Or.newInstance(disjunctionConstraints);
            backtrackAfter++;
            addConstraintAfterNewBacktrackingPoint(newConstraint);
            if (isSatisfiable()) {
                Labels newLabels = LabelUtility.getLabels(
                        this,
                        mulibValueLabeler,
                        l.getIdToNamedVar()
                );
                Object solutionValue = latestSolution.returnValue;
                if (solutionValue instanceof Sym) {
                    solutionValue = l.getLabelForNamedSubstitutedVar((SubstitutedVar) solutionValue);
                }
                Solution newSolution = new Solution(
                        solutionValue,
                        newLabels
                );
                currentN = N.decrementAndGet();
                solutions.add(newSolution);
                latestSolution = newSolution;
            } else {
                break;
            }
        }
        backtrack(backtrackAfter);
        solutions.add(initialSolution);
        return solutions;
    }

    protected static Constraint getNeq(SubstitutedVar sv, Object value) {
        if (sv instanceof Conc) {
            return Sbool.ConcSbool.FALSE;
        }
        if (sv instanceof Sbool) {
            Sbool bv = (Sbool) sv;
            Sbool bvv = Sbool.concSbool((boolean) value);
            return Xor.newInstance(bv, bvv);
        }
        if (sv instanceof Snumber) {
            Snumber wrappedPreviousValue;
            if (value instanceof Integer) {
                wrappedPreviousValue = Sint.concSint((Integer) value);
            } else if (value instanceof Double) {
                wrappedPreviousValue = Sdouble.concSdouble((Double) value);
            } else if (value instanceof Float) {
                wrappedPreviousValue = Sfloat.concSfloat((Float) value);
            } else if (value instanceof Long) {
                wrappedPreviousValue = Slong.concSlong((Long) value);
            } else if (value instanceof Short) {
                wrappedPreviousValue = Sshort.concSshort((Short) value);
            } else if (value instanceof Byte) {
                wrappedPreviousValue = Sbyte.concSbyte((Byte) value);
            } else {
                throw new NotYetImplementedException(sv.getClass().toString());
            }
            return Not.newInstance(Eq.newInstance((Snumber) sv, wrappedPreviousValue));
        } else {
            throw new NotYetImplementedException();
        }
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
