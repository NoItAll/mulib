package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.choice_points.CoverageCfg;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.ChoiceOptionDeque;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.lang.invoke.MethodHandle;
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
            SearchStrategy searchStrategy,
            MethodHandle searchRegionMethod,
            StaticVariables staticVariables,
            Object[] searchRegionArgs) {
        super(mulibExecutorManager, mulibValueTransformer, config, rootChoiceOption, searchStrategy,
                searchRegionMethod, staticVariables, searchRegionArgs);
        Function<ChoiceOptionDeque, Optional<Choice.ChoiceOption>> choiceOptionDequeRetriever;
        if (searchStrategy == SearchStrategy.DFS) {
            this.continueExecution = () -> true;
            choiceOptionDequeRetriever = GenericExecutor::dfsRetriever;
        } else if (searchStrategy == SearchStrategy.BFS) {
            this.continueExecution = () -> false;
            choiceOptionDequeRetriever = GenericExecutor::bfsRetriever;
        } else if (searchStrategy == SearchStrategy.IDDFS) {
            this.continueExecution = () -> true;
            choiceOptionDequeRetriever = GenericExecutor::bfsRetriever;
        } else if (searchStrategy == SearchStrategy.DSAS) {
            this.continueExecution = () -> true;
            choiceOptionDequeRetriever = this::dsasRetriever;
        } else if (searchStrategy == SearchStrategy.IDDSAS) {
            this.continueExecution = this::continueBasedOnGlobalIddfs;
            choiceOptionDequeRetriever = this::dsasRetriever;
        } else {
            throw new NotYetImplementedException();
        }
        if (config.CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE) {
            final Function<ChoiceOptionDeque, Optional<Choice.ChoiceOption>> fallback = choiceOptionDequeRetriever;
            choiceOptionDequeRetriever = (coDeque) -> {
                Optional<Choice.ChoiceOption> co = this.cfgRetriever(coDeque);
                if (co.isEmpty()) {
                    return fallback.apply(coDeque);
                }
                return co;
            };
        }
        this.choiceOptionDequeRetriever = choiceOptionDequeRetriever;
    }

    private boolean continueBasedOnGlobalIddfs() {
        int currentDepth = currentChoiceOption.getDepth();
        int toReach = getExecutorManager().globalIddfsSynchronizer.getToReachDepth();
        // Should we simply continue since the current depth is less then the depth to be reached?
        if (currentDepth <= toReach) {
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
    public LinkedHashMap<String, String> getStatistics() {
        LinkedHashMap<String, String> result = super.getStatistics();
        if (searchStrategy == SearchStrategy.DSAS) {
            result.put("missedDsas", String.valueOf(dsasMissed));
        }
        return result;
    }

    @Override
    protected boolean shouldContinueExecution() {
        return continueExecution.get();
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

    private Optional<Choice.ChoiceOption> cfgRetriever(ChoiceOptionDeque choiceOptionDeque) {
        final int maximumAttemptsToSearchForChoiceOptionWithUncoveredEdge = 6;
        CoverageCfg cfg = mulibExecutorManager.getCoverageCfg();
        Choice choiceOfPotentialDeepestSharedRoot = currentChoiceOption.getChoice();
        int i = 0;
        while (choiceOfPotentialDeepestSharedRoot != rootChoiceOfSearchTree
                && i < maximumAttemptsToSearchForChoiceOptionWithUncoveredEdge) {
            // Check the current Choice's choice options
            for (Choice.ChoiceOption co : choiceOfPotentialDeepestSharedRoot.getChoiceOptions()) {
                // We do not need to check the choice options in the Deque if the ChoiceOption already was evaluated.
                if (co.isEvaluated()) {
                    continue;
                }
                if (!cfg.hasUncoveredEdges(co)) {
                    i++;
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
        return Optional.empty();
    }
}
