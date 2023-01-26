package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.budget.ExecutionBudgetManager;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.ChoiceOptionDeque;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class GenericExecutor extends AbstractMulibExecutor {
    private final Function<ChoiceOptionDeque, Optional<Choice.ChoiceOption>> choiceOptionDequeRetriever;
    private final Supplier<Boolean> continueExecution;
    private long dsasMissed;

    public GenericExecutor(
            Choice.ChoiceOption rootChoiceOption,
            MulibExecutorManager mulibExecutorManager,
            MulibValueTransformer mulibValueTransformer,
            MulibConfig config,
            SearchStrategy searchStrategy) {
        super(mulibExecutorManager, mulibValueTransformer, config, rootChoiceOption, searchStrategy);
        if (searchStrategy == SearchStrategy.DFS) {
            this.continueExecution = () -> true;
            this.choiceOptionDequeRetriever = GenericExecutor::dfsRetriever;
        } else if (searchStrategy == SearchStrategy.BFS) {
            this.continueExecution = () -> false;
            this.choiceOptionDequeRetriever = GenericExecutor::bfsRetriever;
        } else if (searchStrategy == SearchStrategy.IDDFS) {
            this.continueExecution = () -> true;
            this.choiceOptionDequeRetriever = GenericExecutor::bfsRetriever;
        } else if (searchStrategy == SearchStrategy.DSAS) {
            this.continueExecution = () -> true;
            this.choiceOptionDequeRetriever = this::dsasRetriever;
        } else if (searchStrategy == SearchStrategy.IDDSAS) {
            this.continueExecution = this::continueBasedOnGlobalIddfs;
            this.choiceOptionDequeRetriever = this::dsasRetriever;
        } else {
            throw new NotYetImplementedException();
        }
    }

    private boolean continueBasedOnGlobalIddfs() {
        int currentDepth = currentChoiceOption.getDepth();
        int toReach = getExecutorManager().globalIddfsSynchronizer.getToReachDepth();
        // Should we simply continue since the current depth is less then the depth to be reached?
        if (currentDepth < toReach) {
            return true;
        }
        // Check if we should increase the depth to reach
        int[] minMaxDepth = getDeque().getMinMaxDepth();
        if (minMaxDepth[0] == minMaxDepth[1]) {
            getExecutorManager().globalIddfsSynchronizer.setNextDepth(currentDepth);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Optional<Choice.ChoiceOption> chooseNextChoiceOption(List<Choice.ChoiceOption> options) {
        Choice.ChoiceOption result = null;
        ExecutionBudgetManager ebm = currentSymbolicExecution.getExecutionBudgetManager();
        boolean isActualIncrementalBudgetExceeded =
                ebm.incrementalActualChoicePointBudgetIsExceeded();
        if (continueExecution.get()) {
            for (Choice.ChoiceOption choiceOption : options) {
                if (checkIfSatisfiableAndSet(choiceOption)) {
                    result = choiceOption;
                    break;
                }
            }
        }
        if (terminated || result == null || isActualIncrementalBudgetExceeded) {
            backtrackOnce();
            // Optional.empty() means backtracking. Is used in ChoicePointFactory.
            return Optional.empty();
        } else {
            return Optional.of(result);
        }
    }

    @Override
    public LinkedHashMap<String, String> getStatistics() {
        LinkedHashMap<String, String> result = super.getStatistics();
        if (searchStrategy == SearchStrategy.DSAS) {
            result.put("missedDsas", String.valueOf(dsasMissed));
        }
        return result;
    }

    @Override
    protected Optional<Choice.ChoiceOption> selectNextChoiceOption(ChoiceOptionDeque deque) {
        return choiceOptionDequeRetriever.apply(deque);
    }

    private static Optional<Choice.ChoiceOption> dfsRetriever(ChoiceOptionDeque choiceOptionDeque) {
        return choiceOptionDeque.pollLast();
    }

    private static Optional<Choice.ChoiceOption> bfsRetriever(ChoiceOptionDeque choiceOptionDeque) {
        return choiceOptionDeque.pollFirst();
    }

    private Optional<Choice.ChoiceOption> dsasRetriever(ChoiceOptionDeque choiceOptionDeque) {
        Choice choiceOfPotentialDeepestSharedRoot = currentChoiceOption.getChoice();
        while (choiceOfPotentialDeepestSharedRoot != rootChoiceOfSearchTree) {
            // Check the current Choice's choice options
            for (Choice.ChoiceOption co : choiceOfPotentialDeepestSharedRoot.getChoiceOptions()) {
                // We do not need to check the choice options in the Deque if the ChoiceOption already was evaluated.
                if (co.isEvaluated()) {
                    continue;
                }
                // If we find the ChoiceOption in the Deque, we return it.
                if (choiceOptionDeque.request(co)) {
                    return Optional.of(co);
                }
            }
            // Backtrack
            choiceOfPotentialDeepestSharedRoot = choiceOfPotentialDeepestSharedRoot.parent.getChoice();
        }
        dsasMissed++;
        return choiceOptionDeque.pollFirst();
    }
}
