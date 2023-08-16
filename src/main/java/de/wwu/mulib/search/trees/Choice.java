package de.wwu.mulib.search.trees;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectConstraint;
import de.wwu.mulib.exceptions.IllegalTreeAccessException;
import de.wwu.mulib.exceptions.IllegalTreeModificationException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.budget.Budget;
import de.wwu.mulib.solving.Solution;

import java.util.*;

/**
 * Represents a choice with a list of {@link ChoiceOption} as its children in the {@link SearchTree}.
 * Typically, two choice options are contained in on choice. However, more are possible.
 */
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

    /**
     * Represents a choice option with a constraint in the search tree.
     * A choice option can have a set of states, as represented by {@link ChoiceOption#isSatisfiable()} etc.
     * These states are used to track whether we can still modify a choice option and whether we have already checked if it
     * is satisfiable or not.
     * The constraints of a choice option can be mutated via {@link ChoiceOption#setOptionConstraint(Constraint)} and
     * {@link ChoiceOption#addPartnerClassConstraint(PartnerClassObjectConstraint)}. This can only be done if the choice
     * option has not yet been evaluated. A choice option might get unsatisfiable due to such added constraints.
     */
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
        private static final byte CONSTRAINT_MODIFIED_AFTER_INITIAL_SAT_CHECK = -128;

        private byte state;
        /**
         * The number of the choice option. If the parent choice is binary it is expected that the constraint which
         * causes the execution to fork via a {@link Choice} has the number 0 while the negation has the number 1.
         */
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

        /**
         * @return The depth of this option's choice in the search tree
         */
        public int getDepth() {
            return Choice.this.depth;
        }

        /**
         * Sets the child of this choice option to the specified tree node.
         * Throws an exception if the choice option is either already evaluated.
         * @param child The child
         */
        void setChild(TreeNode child) {
            _checkChildIsUnset();
            _checkIllegalStateModificationElseSet(EVALUATED);
            this.child = child;
        }

        /**
         * @return The parent of this choice option
         */
        public ChoiceOption getParent() {
            return Choice.this.parent;
        }

        /**
         * @return The choice this option is a child of
         */
        public Choice getChoice() {
            return Choice.this;
        }

        /**
         * @return The constraint associated with this choice option
         */
        public Constraint getOptionConstraint() {
            return optionConstraint;
        }

        /**
         * @return The list of partner class constraints that were added while evaluating this choice option
         */
        public List<PartnerClassObjectConstraint> getPartnerClassObjectConstraints() {
            return partnerClassObjectConstraints;
        }

        private boolean isIllegalConstraintModification() {
            return child != null || isEvaluated();
        }

        /**
         * Sets the choice option constraint.
         * Heuristics can use this to set the constraint to {@link de.wwu.mulib.substitutions.primitives.Sbool.ConcSbool#TRUE}
         * or {@link de.wwu.mulib.substitutions.primitives.Sbool.ConcSbool#FALSE} if this can be proven.
         * Throws an exception if this choice option is already evaluated.
         * @param optionConstraint The new constraint
         */
        public void setOptionConstraint(Constraint optionConstraint) {
            if (isIllegalConstraintModification()) {
                throw new IllegalTreeModificationException("The constraint of a choice option that has already been" +
                        " evaluated cannot be changed");
            }
            assert isSatisfiable();
            state |= CONSTRAINT_MODIFIED_AFTER_INITIAL_SAT_CHECK;
            this.optionConstraint = optionConstraint;
        }

        /**
         * Adds a new partner class constraint.
         * Throws an exception if this choice option was already evaluated.
         * @param ic The new partner class constraint
         */
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

        /**
         * @return The child, if a child has already been set. If there is no child, an exception is thrown.
         */
        public TreeNode getChild() {
            if (child == null) {
                throw new IllegalTreeAccessException("child cannot be retrieved as it has not yet been set.");
            }
            return child;
        }

        /**
         * Sets the choice option to be unsatisfiable
         * Throws an exception if this choice option was already evaluated.
         * @return The Fail node associated with the failure of this choice option
         */
        @SuppressWarnings("UnusedReturnValue")
        public Fail setUnsatisfiable() {
            _checkChildIsUnset();
            Fail result = new Fail(this, false);
            // Fail(...) will automatically set state to EVALUATED, revert this.
            state = UNSATISFIABLE;
            return result;
        }

        /**
         * Sets the choice option to be explicitly failed, i.e., the user has thrown a {@link de.wwu.mulib.Fail}.
         * This yields a leaf node in the search tree.
         * Throws an exception if this choice option was already evaluated.
         * @return The Fail node associated with the failure of this choice option
         */
        public Fail setExplicitlyFailed() {
            _checkChildIsUnset();
            Fail result = new Fail(this, true);
            // Fail(...) will automatically set state to EVALUATED, revert this.
            state = EXPLICITLY_FAILED;
            return result;
        }

        /**
         * Sets the choice option to yield a leaf node of the search tree, a {@link PathSolution}
         * All constraints on the path are added to the path solution
         * Throws an exception if this choice option was already evaluated.
         * @param s The solution to wrap in the path solution
         * @param constraints The encountered constraints
         * @param partnerClassObjectConstraints The list of partner class constraints
         * @return The PathSolution node associated with reaching a leaf node
         */
        public PathSolution setSolution(Solution s, Constraint[] constraints, PartnerClassObjectConstraint[] partnerClassObjectConstraints) {
            _checkChildIsUnset();
            return new PathSolution(this, s, constraints, partnerClassObjectConstraints);
        }

        /**
         * Sets the choice option to yield a leaf node of the search tree, a {@link PathSolution}
         * All constraints on the path are added to the path solution
         * Throws an exception if this choice option was already evaluated.
         * @param s The solution to wrap in the path solution
         * @param constraints The encountered constraints
         * @param partnerClassObjectConstraints The list of partner class constraints
         * @param cover The cover that could be calculated for this path solution. Is, e.g., calculated using {@link de.wwu.mulib.search.choice_points.CoverageCfg}.
         * @return The path solution node associated with reaching a leaf node. The path solution also contains a coverage bit set
         */
        public PathSolutionWithCover setSolution(Solution s, Constraint[] constraints, PartnerClassObjectConstraint[] partnerClassObjectConstraints, BitSet cover) {
            _checkChildIsUnset();
            return new PathSolutionWithCover(this, s, constraints, partnerClassObjectConstraints, cover);
        }

        /**
         * Sets the choice option to yield a leaf node of the search tree, a {@link PathSolution}.
         * The returned path solution is an {@link ExceptionPathSolution}, i.e., an exception was thrown.
         * All constraints on the path are added to the path solution
         * Throws an exception if this choice option was already evaluated.
         * @param s The solution to wrap in the path solution
         * @param constraints The encountered constraints
         * @param partnerClassObjectConstraints The list of partner class constraints
         * @return The path solution node associated with reaching a leaf node
         */
        public ExceptionPathSolution setExceptionSolution(Solution s, Constraint[] constraints, PartnerClassObjectConstraint[] partnerClassObjectConstraints) {
            _checkChildIsUnset();
            return new ExceptionPathSolution(this, s, constraints, partnerClassObjectConstraints);
        }

        /**
         * Sets the choice option to yield a leaf node of the search tree, a {@link PathSolution}.
         * The returned path solution is an {@link ExceptionPathSolution}, i.e., an exception was thrown.
         * All constraints on the path are added to the path solution
         * Throws an exception if this choice option was already evaluated.
         * @param s The solution to wrap in the path solution
         * @param constraints The encountered constraints
         * @param partnerClassObjectConstraints The list of partner class constraints
         * @param cover The cover that could be calculated for this path solution. Is, e.g., calculated using {@link de.wwu.mulib.search.choice_points.CoverageCfg}.
         * @return The path solution node associated with reaching a leaf node. The path solution also contains a coverage bit set
         */
        public ExceptionPathSolutionWithCover setExceptionSolution(Solution s, Constraint[] constraints, PartnerClassObjectConstraint[] partnerClassObjectConstraints, BitSet cover) {
            _checkChildIsUnset();
            return new ExceptionPathSolutionWithCover(this, s, constraints, partnerClassObjectConstraints, cover);
        }

        /**
         * Sets the choice option to yield a leaf node in the search tree, a {@link ExceededBudget}.
         * Throws an exception if this choice option was already evaluated.
         * @param budget The budget that was exceeded
         * @return The exceeded budget node associated with reaching a leaf node
         */
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

        /**
         * @return true, if the choice option is evaluated, i.e., a child node has been found, else false
         */
        public boolean isEvaluated() {
            return (state & EVALUATED) != 0;
        }

        /**
         * @return true, if the constraints added by this choice option cause the constraint stack of the
         * {@link de.wwu.mulib.solving.solvers.SolverManager} to be unsatisfiable, else false
         */
        public boolean isUnsatisfiable() {
            return (state & UNSATISFIABLE) != 0;
        }

        /**
         * @return true, if the choice option is explicitly failed, i.e., the user caused this branch to not be further
         * evaluated, else false
         */
        public boolean isExplicitlyFailed() {
            return (state & EXPLICITLY_FAILED) != 0;
        }

        /**
         * @return true, if the choice option is found to be satisfiable. Note that a choice option can become unsatisfiable
         * due to constraints added via {@link ChoiceOption#setOptionConstraint(Constraint)} later on. This update is
         * not necessarily reflected via this return value. Else false
         */
        public boolean isSatisfiable() {
            return (state & SATISFIABLE) != 0;
        }

        /**
         * @return true, if the choice option yields a leaf node since it exceeds a budget, else false
         */
        public boolean isBudgetExceeded() {
            return (state & BUDGET_EXCEEDED) != 0;
        }

        /**
         * @return true, if the choice option and all children are excluded from further evaluation, else false
         */
        public boolean isCutOff() {
            return (state & CUT_OFF) != 0;
        }

        /**
         * @return true if there is no current information on the choice option available, else false
         */
        public boolean isUnknown() {
            return state == UNKNOWN || state == CONSTRAINT_MODIFIED_AFTER_INITIAL_SAT_CHECK;
        }

        /**
         * Sets the current choice option to be satisfiable.
         * Throws an exception if the state transition is illegal
         */
        public void setSatisfiable() {
            _checkIllegalStateModificationElseSet(SATISFIABLE);
        }

        /**
         * @return true, if the choice option was modified after the intial SAT check, e.g., by changing the associated
         * constraint
         */
        public boolean constraintWasModifiedAfterInitialSatCheck() {
            return (state & CONSTRAINT_MODIFIED_AFTER_INITIAL_SAT_CHECK) != 0;
        }

        private void _checkIllegalStateModificationElseSet(byte newState) {
            if (!isUnknown()
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

        /**
         * @return An informative string on the current state of the choice option
         */
        public String stateToString() {
            return stateToString(state);
        }

        /**
         * @param state The state to get information on
         * @return An informative string on the meaning of the state
         */
        public String stateToString(byte state) {
            return (state == UNKNOWN || state == CONSTRAINT_MODIFIED_AFTER_INITIAL_SAT_CHECK) ? "UNKNOWN" :
                    ((state & SATISFIABLE) != 0) ? "SATISFIABLE" :
                            ((state & EVALUATED) != 0) ? "EVALUATED" :
                                    ((state & BUDGET_EXCEEDED) != 0) ? "BUDGET_EXCEEDED" :
                                            ((state & CUT_OFF) != 0) ? "CUT_OFF" :
                                                    ((state & EXPLICITLY_FAILED) != 0) ? "EXPLICITLY_FAILED" :
                                                            ((state & UNSATISFIABLE) != 0) ? "UNSATISFIABLE" :
                                                                    "UNKNOWN_STATE";
        }

    }
}
