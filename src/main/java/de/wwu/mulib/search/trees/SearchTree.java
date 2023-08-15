package de.wwu.mulib.search.trees;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectConstraint;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.primitives.Sbool;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a search tree.
 */
public final class SearchTree {

    private final MulibConfig config;
    /**
     * The root node of the search tree. Initially this is a choice with a single choice option having the constraint
     * {@link Sbool.ConcSbool#TRUE}.
     */
    public final Choice root;
    /**
     * The path solutions of this search tree
     */
    private final List<PathSolution> solutionsList;
    /**
     * If configured, stores all encountered fail nodes explicitly
     */
    private final List<Fail> failsList;
    /**
     * If configured, stores all exceeded budgets explicitly
     */
    private final List<ExceededBudget> exceededBudgetList;
    /**
     * A double-ended priority "deque" storing the choice options that can be explored in this search tree
     */
    private final ChoiceOptionDeque choiceOptionDeque;
    private final String indentBy;
    private final boolean enlistLeaves;

    /**
     * @param config The configuration
     */
    public SearchTree(
            MulibConfig config) {
        this.config = config;
        this.indentBy = config.TREE_INDENTATION;
        this.enlistLeaves = config.ENLIST_LEAVES;
        this.root = new Choice(null, Sbool.ConcSbool.TRUE);
        this.root.getOption(0).setSatisfiable();
        if (!config.ADDITIONAL_PARALLEL_SEARCH_STRATEGIES.isEmpty()) {
            solutionsList = Collections.synchronizedList(new ArrayList<>());
            if (enlistLeaves) {
                failsList = Collections.synchronizedList(new ArrayList<>());
                exceededBudgetList = Collections.synchronizedList(new ArrayList<>());
            } else {
                failsList = null;
                exceededBudgetList = null;
            }
        } else {
            solutionsList = new ArrayList<>();
            if (enlistLeaves) {
                failsList = new ArrayList<>();
                exceededBudgetList = new ArrayList<>();
            } else {
                failsList = null;
                exceededBudgetList = null;
            }
        }
        choiceOptionDeque = ChoiceOptionDeques.getChoiceOptionDeque(config, root.getOption(0));
    }

    /**
     * Returns the trail to a choice option. This is used by {@link de.wwu.mulib.search.executors.SymbolicExecution} to
     * retrieve the predetermined choices to reach an unexplored choice option.
     * @param getTo The choice option that should be explored in the search region
     * @return An ArrayDeque with the path/trail of choice options that has to be taken to reach the choice option 'getTo'
     */
    public static ArrayDeque<Choice.ChoiceOption> getPathTo(final Choice.ChoiceOption getTo) {
        ArrayDeque<Choice.ChoiceOption> result = new ArrayDeque<>();
        Choice.ChoiceOption currentChoiceOption = getTo;
        while (currentChoiceOption != null) {
            result.push(currentChoiceOption);
            currentChoiceOption = currentChoiceOption.getParent();
        }
        return result;
    }

    /**
     * Returns the choice option deepest in the search tree that is shared by the two choice options.
     * Is used by {@link de.wwu.mulib.search.executors.AbstractMulibExecutor} to determine how often the
     * {@link de.wwu.mulib.solving.solvers.SolverManager} must be backtracked so that a new choice option can be targeted.
     * @param co0 The first choice option
     * @param co1 The second choice option
     * @return The deepest shared ancestor of the co0 and co1
     */
    public static Choice.ChoiceOption getDeepestSharedAncestor(
            Choice.ChoiceOption co0,
            Choice.ChoiceOption co1) {
        if (co0.getParent() == null) {
            return co0;
        } else if (co1.getParent() == null) {
            return co1;
        }
        while (co0 != co1) {
            if (co0.getDepth() < co1.getDepth()) {
                co1 = co1.getParent();
            } else {
                co0 = co0.getParent();
            }
        }
        return co0;
    }

    /**
     * Get the ChoiceOptions between getFrom and getTo. Assumes that there is a path with strictly increasing depth
     * between the two ChoiceOptions. Will throw an exception if there is no such path.
     * @param getFrom First ChoiceOption, depth <= getTo.depth.
     * @param getTo Second ChoiceOption, depth >= getFrom.depth
     * @return The path between (excluding) the ChoiceOption with the lesser depth to the ChoiceOption with the higher depth.
     */
    public static ArrayDeque<Choice.ChoiceOption> getPathBetween(
            final Choice.ChoiceOption getFrom,
            final Choice.ChoiceOption getTo) {
        ArrayDeque<Choice.ChoiceOption> result = new ArrayDeque<>();
        if (getFrom == getTo) {
            return result;
        }
        assert getTo.getDepth() > getFrom.getDepth();
        Choice.ChoiceOption start = getTo;
        start = start.getParent();
        // Construct path by following parent-path from start to end
        while (start != getFrom) {
            result.addFirst(start);
            start = start.getParent();
        }

        return result;
    }

    /**
     * Accumulates the encountered constraints for traversing from the root of the search tree to the passed choice option.
     * @param co The choice option up to which all constraints shall be collected
     * @return A container with all {@link Constraint}s and {@link PartnerClassObjectConstraint}s encountered
     * on the path to the choice option.
     */
    public static AccumulatedChoiceOptionConstraints getAllConstraintsForChoiceOption(Choice.ChoiceOption co) {
        ArrayDeque<Choice.ChoiceOption> cos = getPathTo(co);
        List<Constraint> constraints = new ArrayList<>();
        List<PartnerClassObjectConstraint> partnerClassObjectConstraints = new ArrayList<>();
        while (!cos.isEmpty()) {
            Choice.ChoiceOption current = cos.poll();
            constraints.add(current.getOptionConstraint());
            partnerClassObjectConstraints.addAll(current.getPartnerClassObjectConstraints());
        }
        return new AccumulatedChoiceOptionConstraints(
                constraints.toArray(Constraint[]::new),
                partnerClassObjectConstraints.toArray(PartnerClassObjectConstraint[]::new)
        );
    }

    /**
     * Container for {@link Constraint}s and {@link PartnerClassObjectConstraint}s
     */
    public static class AccumulatedChoiceOptionConstraints {
        /**
         * The constraints
         */
        public final Constraint[] constraints;
        /**
         * The partner class object constraints
         */
        public final PartnerClassObjectConstraint[] partnerClassObjectConstraints;

        AccumulatedChoiceOptionConstraints(Constraint[] constraints, PartnerClassObjectConstraint[] partnerClassObjectConstraints) {
            this.constraints = constraints;
            this.partnerClassObjectConstraints = partnerClassObjectConstraints;
        }

    }

    /**
     * @return A string representation of this search tree. We purposefully did not use toString since this
     * kills the debugger if the search tree is sufficiently large.
     */
    public String stringRepresentation() {
        return toString(root, indentBy);
    }

    private String toString(TreeNode currentNode, String indentBy) {
        StringBuilder sb = new StringBuilder();
        if (currentNode instanceof Choice) {
            Choice choice = (Choice) currentNode;
            for (Choice.ChoiceOption co : choice.getChoiceOptions()) {
                sb.append(indentBy.repeat(currentNode.depth));
                sb.append("- ChoiceOption(").append(choice.depth).append(")").append(co.getOptionConstraint());
                if (!co.getPartnerClassObjectConstraints().isEmpty()) {
                    sb.append(co.getPartnerClassObjectConstraints());
                }
                sb.append("\r\n");
                if (co.isEvaluated()) {
                    sb.append(toString(co.getChild(), indentBy));
                } else {
                    sb.append(indentBy.repeat(currentNode.depth + 1)).append(co.stateToString()).append("\r\n");
                }
            }
        } else {
            sb.append(indentBy.repeat(currentNode.depth));
            if (currentNode instanceof Fail) {
                sb.append("- Fail");
            } else if (currentNode instanceof PathSolution) {
                sb.append("- PathSolution: ").append(currentNode);
            } else if (currentNode instanceof ExceededBudget) {
                sb.append("- ExceededBudget");
            } else {
                throw new NotYetImplementedException();
            }
            sb.append("\r\n");
        }
        return sb.toString();
    }

    /**
     * @return The choice option deque
     */
    public ChoiceOptionDeque getChoiceOptionDeque() {
        return choiceOptionDeque;
    }

    /**
     * Adds a path solution to the list of path solutions
     * @param pathSolution The path solution
     */
    public void addToPathSolutions(PathSolution pathSolution) {
        this.solutionsList.add(pathSolution);
    }

    /**
     * @return The path solutions list
     */
    public List<PathSolution> getPathSolutionsList() {
        return solutionsList;
    }

    /**
     * @return The fails list
     */
    public List<Fail> getFailsList() {
        return failsList;
    }

    /**
     * @return The exceeded budgets list
     */
    public List<ExceededBudget> getExceededBudgetList() {
        return exceededBudgetList;
    }

    /**
     * Adds a fail to the list of fails, if configured to do so
     * @param fail The fail
     */
    public void addToFails(Fail fail) {
        if (enlistLeaves) this.failsList.add(fail);
    }

    /**
     * Adds an exceeded budget to the list of exceeded budgets, if configured to do so
     * @param exceededBudget The exceeded budget
     */
    public void addToExceededBudgets(ExceededBudget exceededBudget) {
        if (enlistLeaves) this.exceededBudgetList.add(exceededBudget);
    }

}
