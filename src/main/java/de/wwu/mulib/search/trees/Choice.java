package de.wwu.mulib.search.trees;

import de.wwu.mulib.constraints.ArrayConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.IllegalTreeAccessException;
import de.wwu.mulib.exceptions.IllegalTreeModificationException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.budget.Budget;
import de.wwu.mulib.solving.Labels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class Choice extends TreeNode {
    private final List<ChoiceOption> options;

    public Choice(ChoiceOption parent, Collection<Constraint> constraintsPerOption) {
        this(parent, constraintsPerOption.toArray(new Constraint[0]));
    }

    // If there are only two constraints which are opposites, i.e. c and Not(c), the first constraint should be
    // c, the second constraint should be Not(c).
    public Choice(ChoiceOption parent, Constraint... constraintPerOption) {
        super(parent);
        if (constraintPerOption.length < 1) {
            throw new IllegalTreeModificationException("There must be at least one choice option for a choice.");
        }
        final int numberOfOptions = constraintPerOption.length;
        ChoiceOption[] optionsAr = new ChoiceOption[numberOfOptions];
        for (int i = 0; i < numberOfOptions; i++) {
            optionsAr[i] = new ChoiceOption(i, constraintPerOption[i]);
        }
        options = List.of(optionsAr);
    }

    public ChoiceOption getOption(int choiceNumber) {
        return options.get(choiceNumber);
    }

    public List<ChoiceOption> getChoiceOptions() {
        return options;
    }

    @Override
    public String toString() {
        return "Choice{depth=" + depth + ",nrOptions=" + options.size() + "}";
    }

    public final class ChoiceOption {
        // No information on current state
        public static final byte UNKNOWN = 0;
        // Can be validly evaluated
        public static final byte SATISFIABLE = 1;
        // To be evaluated, the option must have been satisfiable
        public static final byte EVALUATED = 2;
        // Symbolic state connected with tree is not satisfiable
        public static final byte UNSATISFIABLE = 3;
        // Further evaluation would exceed budget
        public static final byte BUDGET_EXCEEDED = 4;
        // The further evaluation of the respective subtree will not be performed
        public static final byte CUT_OFF = 5; // TODO not yet implemented functionality
        // If Mulib.failed() has been used.
        public static final byte EXPLICITLY_FAILED = 6;
        // For concolic execution: if the labeling is not valid anymore, we need to reevaluate
        public static final byte REEVALUATION_NEEDED = 7;

        private byte state;
        public final int choiceOptionNumber;
        // This constraint must be fulfilled to further evaluate the ChoiceOption
        private Constraint optionConstraint;
        // Separate ArrayConstraints. These are added after the fact.
        private List<ArrayConstraint> arrayConstraints = Collections.EMPTY_LIST;

        // The possible child of this ChoiceOption is set after evaluating the option.
        private TreeNode child = null;

        private ChoiceOption(int choiceOptionNumber, Constraint optionConstraint) {
            this.choiceOptionNumber = choiceOptionNumber;
            this.optionConstraint = optionConstraint;
        }

        public int getDepth() {
            return Choice.this.depth;
        }

        void setChild(TreeNode child) {
            _checkChildIsUnset();
            _checkIllegalStateModificationElseSet(EVALUATED);
            this.child = child;
        }

        public ChoiceOption getParent() {
            return Choice.this.parent;
        }

        public Choice getChoice() {
            return Choice.this;
        }

        public Constraint getOptionConstraint() {
            return optionConstraint;
        }

        public List<ArrayConstraint> getArrayConstraints() {
            return arrayConstraints;
        }

        private boolean isIllegalConstraintModification() {
            return child != null || isEvaluated();
        }

        public void setOptionConstraint(Constraint optionConstraint) {
            if (isIllegalConstraintModification()) {
                throw new IllegalTreeModificationException("The constraint of a choice option that has already been" +
                        " evaluated cannot be changed");
            }
            this.optionConstraint = optionConstraint;
        }

        public void addArrayConstraint(ArrayConstraint arrayConstraint) {
            if (isIllegalConstraintModification()) {
                throw new IllegalTreeModificationException("The array constraint must not be added to already evaluated" +
                        " choice options");
            }
            if (arrayConstraints == Collections.EMPTY_LIST) {
                arrayConstraints = new ArrayList<>();
            }
            if (getDepth() != arrayConstraint.getLevel()) {
                throw new IllegalTreeModificationException("The ArrayConstraint must be defined on the same depth like the" +
                        " choice option");
            }
            arrayConstraints.add(arrayConstraint);
        }

        public TreeNode getChild() {
            if (child == null) {
                throw new IllegalTreeAccessException("child cannot be retrieved as it has not yet been set.");
            }
            return child;
        }

        @SuppressWarnings("UnusedReturnValue")
        public Fail setUnsatisfiable() {
            _checkChildIsUnset();
            Fail result = new Fail(this, false);
            // Fail(...) will automatically set state to EVALUATED, revert this.
            state = UNSATISFIABLE;
            return result;
        }

        public Fail setExplicitlyFailed() {
            _checkChildIsUnset();
            Fail result = new Fail(this, true);
            // Fail(...) will automatically set state to EVALUATED, revert this.
            state = EXPLICITLY_FAILED;
            return result;
        }

        public PathSolution setSolution(Object value, Labels l, Constraint[] constraints, ArrayConstraint[] arrayConstraints) {
            _checkChildIsUnset();
            return new PathSolution(this, value, l, constraints, arrayConstraints);
        }

        public ExceptionPathSolution setExceptionSolution(Throwable throwable, Labels labels, Constraint[] constraints, ArrayConstraint[] arrayConstraints) {
            _checkChildIsUnset();
            return new ExceptionPathSolution(this, throwable, labels, constraints, arrayConstraints);
        }

        public ExceededBudget setBudgetExceeded(Budget budget) {
            _checkChildIsUnset();
            if (budget.isIncremental()) {
                throw new MulibRuntimeException("setBudgetExceeded must not be called for incremental budgets.");
            }

            ExceededBudget result = new ExceededBudget(this, budget); // Is set automatically
            // ExceededBudget(...) will automatically set state to EVALUATED, revert this.
            state = BUDGET_EXCEEDED;
            return result;
        }

        private void _checkChildIsUnset() {
            if (child != null || state == EVALUATED) {
                throw new IllegalTreeModificationException("child cannot be set multiple times."
                        + " child is already set to: " + child);
            }
        }

        public boolean isEvaluated() {
            return state == EVALUATED;
        }

        public boolean isUnsatisfiable() {
            return state == UNSATISFIABLE;
        }

        public boolean isExplicitlyFailed() {
            return state == EXPLICITLY_FAILED;
        }

        public boolean isSatisfiable() {
            return state == SATISFIABLE;
        }

        public boolean isBudgetExceeded() {
            return state == BUDGET_EXCEEDED;
        }

        public boolean isCutOff() {
            return state == CUT_OFF;
        }

        public boolean isUnknown() {
            return state == UNKNOWN;
        }

        public void setSatisfiable() {
            _checkIllegalStateModificationElseSet(SATISFIABLE);
        }

        public void setReevaluationNeeded() {
            _checkIllegalStateModificationElseSet(REEVALUATION_NEEDED);
        }

        public boolean reevaluationNeeded() {
            return state == REEVALUATION_NEEDED;
        }

        private void _checkIllegalStateModificationElseSet(byte newState) {
            if (state != UNKNOWN
                    && !(state == SATISFIABLE && newState == REEVALUATION_NEEDED)
                    && !(state == REEVALUATION_NEEDED && (newState == EVALUATED || newState == SATISFIABLE))
                    && !(state == SATISFIABLE && newState == EVALUATED)) {
                throw new IllegalTreeModificationException("New state '" + stateToString(newState) + "' cannot be set. " +
                        "State is already set to '" + stateToString() + "'.");
            }
            state = newState;
        }

        @Override
        public String toString() {
            return "ChoiceOption{depth=" + depth
                    + ",number=" + choiceOptionNumber
                    + ",constraint=" + optionConstraint
                    + ",arrayConstraints=" + arrayConstraints
                    + ",state="
                        + stateToString()
                    + "}";
        }

        private String stateToString(byte state) {
            return state == UNKNOWN ? "UNKNOWN" :
                    state == SATISFIABLE ? "SATISFIABLE" :
                            state == EVALUATED ? "EVALUATED" :
                                    state == BUDGET_EXCEEDED ? "BUDGET_EXCEEDED" :
                                            state == CUT_OFF ? "CUT_OFF" :
                                                    state == EXPLICITLY_FAILED ? "EXPLICITLY_FAILED" :
                                                            state == UNSATISFIABLE ? "UNSATISFIABLE" :
                                                                    state == REEVALUATION_NEEDED ? "REEVALUATION_NEEDED" : "UNKNOWN_STATE";
        }

        public String stateToString() {
            return stateToString(state);
        }

    }
}
