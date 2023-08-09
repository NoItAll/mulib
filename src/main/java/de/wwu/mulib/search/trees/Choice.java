package de.wwu.mulib.search.trees;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectConstraint;
import de.wwu.mulib.exceptions.IllegalTreeAccessException;
import de.wwu.mulib.exceptions.IllegalTreeModificationException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.budget.Budget;

import java.util.*;

public final class Choice extends TreeNode {
    private final List<ChoiceOption> options;

    public Choice(ChoiceOption parent, Collection<Constraint> constraintsPerOption) {
        this(parent, constraintsPerOption.toArray(new Constraint[0]));
    }

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
        private static final byte UNKNOWN = 0;
        // Can be validly evaluated
        private static final byte SATISFIABLE = 1;
        // To be evaluated, the option must have been satisfiable
        private static final byte EVALUATED = 2;
        // Symbolic state connected with tree is not satisfiable
        private static final byte UNSATISFIABLE = 4;
        // Further evaluation would exceed budget
        private static final byte BUDGET_EXCEEDED = 8;
        // The further evaluation of the respective subtree will not be performed
        private static final byte CUT_OFF = 16; // TODO not yet implemented functionality
        // If Mulib.failed() has been used.
        private static final byte EXPLICITLY_FAILED = 32;
        // For concolic execution: if the labeling is not valid anymore, we need to reevaluate
        private static final byte REEVALUATION_NEEDED = 64;
        private static final byte CONSTRAINT_MODIFIED_AFTER_INITIAL_SAT_CHECK = -128;

        private byte state;
        public final int choiceOptionNumber;
        // This constraint must be fulfilled to further evaluate the ChoiceOption
        private Constraint optionConstraint;
        // Separate ArrayConstraints. These are added after the fact.
        @SuppressWarnings("unchecked")
        private List<PartnerClassObjectConstraint> partnerClassObjectConstraints = Collections.EMPTY_LIST;
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

        public List<PartnerClassObjectConstraint> getPartnerClassObjectConstraints() {
            return partnerClassObjectConstraints;
        }

        private boolean isIllegalConstraintModification() {
            return child != null || isEvaluated();
        }

        public void setOptionConstraint(Constraint optionConstraint) {
            if (isIllegalConstraintModification()) {
                throw new IllegalTreeModificationException("The constraint of a choice option that has already been" +
                        " evaluated cannot be changed");
            }
            assert isSatisfiable() || reevaluationNeeded();
            state |= CONSTRAINT_MODIFIED_AFTER_INITIAL_SAT_CHECK;
            this.optionConstraint = optionConstraint;
        }

        public void addPartnerClassConstraint(PartnerClassObjectConstraint ic) {
            if (isIllegalConstraintModification()) {
                throw new IllegalTreeModificationException("The partner class object constraint must not be added to already evaluated" +
                        " choice options");
            }
            if (partnerClassObjectConstraints == Collections.EMPTY_LIST) {
                partnerClassObjectConstraints = new ArrayList<>();
            }
            partnerClassObjectConstraints.add(ic);
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

        public PathSolution setSolution(Solution s, Constraint[] constraints, PartnerClassObjectConstraint[] partnerClassObjectConstraints) {
            _checkChildIsUnset();
            return new PathSolution(this, s, constraints, partnerClassObjectConstraints);
        }

        public PathSolution setSolution(Solution s, Constraint[] constraints, PartnerClassObjectConstraint[] partnerClassObjectConstraints, BitSet cover) {
            _checkChildIsUnset();
            return new PathSolutionWithCover(this, s, constraints, partnerClassObjectConstraints, cover);
        }

        public ExceptionPathSolution setExceptionSolution(Solution s, Constraint[] constraints, PartnerClassObjectConstraint[] partnerClassObjectConstraints) {
            _checkChildIsUnset();
            return new ExceptionPathSolution(this, s, constraints, partnerClassObjectConstraints);
        }

        public ExceptionPathSolution setExceptionSolution(Solution s, Constraint[] constraints, PartnerClassObjectConstraint[] partnerClassObjectConstraints, BitSet cover) {
            _checkChildIsUnset();
            return new ExceptionPathSolutionWithCover(this, s, constraints, partnerClassObjectConstraints, cover);
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
            if (child != null || ((state & EVALUATED) != 0)) {
                throw new IllegalTreeModificationException("child cannot be set multiple times."
                        + " child is already set to: " + child);
            }
        }

        public boolean isEvaluated() {
            return (state & EVALUATED) != 0;
        }

        public boolean isUnsatisfiable() {
            return (state & UNSATISFIABLE) != 0;
        }

        public boolean isExplicitlyFailed() {
            return (state & EXPLICITLY_FAILED) != 0;
        }

        public boolean isSatisfiable() {
            return (state & SATISFIABLE) != 0;
        }

        public boolean isBudgetExceeded() {
            return (state & BUDGET_EXCEEDED) != 0;
        }

        public boolean isCutOff() {
            return (state & CUT_OFF) != 0;
        }

        public boolean isUnknown() {
            return state == UNKNOWN || state == CONSTRAINT_MODIFIED_AFTER_INITIAL_SAT_CHECK;
        }

        public void setSatisfiable() {
            _checkIllegalStateModificationElseSet(SATISFIABLE);
        }

        public void setReevaluationNeeded() {
            _checkIllegalStateModificationElseSet(REEVALUATION_NEEDED);
        }

        public boolean reevaluationNeeded() {
            return (state & REEVALUATION_NEEDED) != 0;
        }

        public boolean constraintWasModifiedAfterInitialSatCheck() {
            return (state & CONSTRAINT_MODIFIED_AFTER_INITIAL_SAT_CHECK) != 0;
        }

        private void _checkIllegalStateModificationElseSet(byte newState) {
            if (!isUnknown()
                    && !(isSatisfiable() && newState == REEVALUATION_NEEDED)
                    && !(reevaluationNeeded() && (newState == EVALUATED /* This is done automatically for FAIL */ || newState == SATISFIABLE))
                    && !(isSatisfiable() && newState == EVALUATED)) {
                throw new IllegalTreeModificationException("New state cannot be set to '" + stateToString(newState) +
                        "'. State is already set to '" + stateToString(state) + "'.");
            }
            byte nextState = (byte) (newState | ((state & CONSTRAINT_MODIFIED_AFTER_INITIAL_SAT_CHECK) != 0 ? CONSTRAINT_MODIFIED_AFTER_INITIAL_SAT_CHECK : 0));
            this.state = nextState;
        }

        @Override
        public String toString() {
            return "ChoiceOption{depth=" + depth
                    + ",number=" + choiceOptionNumber
                    + ",constraint=" + optionConstraint
                    + ",partnerClassObjectConstraints=" + partnerClassObjectConstraints
                    + ",state="
                        + stateToString(state)
                    + "}";
        }

        public String stateToString() {
            return stateToString(state);
        }

        public String stateToString(byte state) {
            return (state == UNKNOWN || state == CONSTRAINT_MODIFIED_AFTER_INITIAL_SAT_CHECK) ? "UNKNOWN" :
                    ((state & SATISFIABLE) != 0) ? "SATISFIABLE" :
                            ((state & EVALUATED) != 0) ? "EVALUATED" :
                                    ((state & BUDGET_EXCEEDED) != 0) ? "BUDGET_EXCEEDED" :
                                            ((state & CUT_OFF) != 0) ? "CUT_OFF" :
                                                    ((state & EXPLICITLY_FAILED) != 0) ? "EXPLICITLY_FAILED" :
                                                            ((state & UNSATISFIABLE) != 0) ? "UNSATISFIABLE" :
                                                                    ((state & REEVALUATION_NEEDED) != 0) ? "REEVALUATION_NEEDED" : "UNKNOWN_STATE";
        }

    }
}
