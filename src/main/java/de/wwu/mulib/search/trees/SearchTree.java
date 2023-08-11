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

public final class SearchTree {

    private final MulibConfig config;
    public final Choice root;
    private final List<PathSolution> solutionsList;
    private final List<Fail> failsList;
    private final List<ExceededBudget> exceededBudgetList;
    private final ChoiceOptionDeque choiceOptionDeque;
    private final String indentBy;
    private final boolean enlistLeaves;

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

    public static ArrayDeque<Choice.ChoiceOption> getPathTo(final Choice.ChoiceOption getTo) {
        ArrayDeque<Choice.ChoiceOption> result = new ArrayDeque<>();
        Choice.ChoiceOption currentChoiceOption = getTo;
        while (currentChoiceOption != null) {
            result.push(currentChoiceOption);
            currentChoiceOption = currentChoiceOption.getParent();
        }
        return result;
    }

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
     * between the two ChoiceOptions.
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

    public static class AccumulatedChoiceOptionConstraints {
        public final Constraint[] constraints;
        public final PartnerClassObjectConstraint[] partnerClassObjectConstraints;

        AccumulatedChoiceOptionConstraints(Constraint[] constraints, PartnerClassObjectConstraint[] partnerClassObjectConstraints) {
            this.constraints = constraints;
            this.partnerClassObjectConstraints = partnerClassObjectConstraints;
        }

    }

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

    public ChoiceOptionDeque getChoiceOptionDeque() {
        return choiceOptionDeque;
    }

    public void addToPathSolutions(PathSolution pathSolution) {
        this.solutionsList.add(pathSolution);
    }

    public List<PathSolution> getPathSolutionsList() {
        return solutionsList;
    }

    public List<Fail> getFailsList() {
        return failsList;
    }

    public List<ExceededBudget> getExceededBudgetList() {
        return exceededBudgetList;
    }

    public void addToFails(Fail fail) {
        if (enlistLeaves) this.failsList.add(fail);
    }

    public void addToExceededBudgets(ExceededBudget exceededBudget) {
        if (enlistLeaves) this.exceededBudgetList.add(exceededBudget);
    }

}
