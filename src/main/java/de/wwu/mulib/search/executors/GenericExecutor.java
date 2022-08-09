package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.budget.ExecutionBudgetManager;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.ChoiceOptionDeque;
import de.wwu.mulib.substitutions.primitives.ValueFactory;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class GenericExecutor extends AbstractMulibExecutor {
    private final Function<ChoiceOptionDeque, Optional<Choice.ChoiceOption>> choiceOptionDequeRetriever;
    private final boolean continueExecution;
    private final ExecutionBudgetManager prototypicalExecutionBudgetManager;
    private long dsasMissed;
    private final MulibValueTransformer mulibValueTransformer;
    private final MulibConfig config;

    public GenericExecutor(
            Choice.ChoiceOption rootChoiceOption,
            MulibExecutorManager mulibExecutorManager,
            MulibValueTransformer mulibValueTransformer,
            MulibConfig config,
            SearchStrategy searchStrategy) {
        super(mulibExecutorManager, config, rootChoiceOption, searchStrategy);
        if (searchStrategy == SearchStrategy.DFS) {
            this.continueExecution = true;
            this.choiceOptionDequeRetriever = GenericExecutor::dfsRetriever;
        } else if (searchStrategy == SearchStrategy.BFS) {
            this.continueExecution = false;
            this.choiceOptionDequeRetriever = GenericExecutor::bfsRetriever;
        } else if (searchStrategy == SearchStrategy.IDDFS) {
            this.continueExecution = true;
            this.choiceOptionDequeRetriever = GenericExecutor::bfsRetriever;
        } else if (searchStrategy == SearchStrategy.DSAS) {
            this.continueExecution = true;
            this.choiceOptionDequeRetriever = this::dsasRetriever;
        } else {
            throw new NotYetImplementedException();
        }
        this.prototypicalExecutionBudgetManager = ExecutionBudgetManager.newInstance(config);
        this.mulibValueTransformer = mulibValueTransformer;
        this.config = config;
    }

    @Override
    public Optional<Choice.ChoiceOption> chooseNextChoiceOption(List<Choice.ChoiceOption> options) {
        Choice.ChoiceOption result = null;
        ExecutionBudgetManager ebm = currentSymbolicExecution.getExecutionBudgetManager();
        boolean isActualIncrementalBudgetExceeded =
                ebm.incrementalActualChoicePointBudgetIsExceeded();
        if (continueExecution) {
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
    protected Optional<SymbolicExecution> createExecution(
            ChoiceOptionDeque deque,
            ChoicePointFactory choicePointFactory,
            ValueFactory valueFactory,
            CalculationFactory calculationFactory) {
        Choice.ChoiceOption optionToBeEvaluated;
        try {
            if (currentChoiceOption.reevaluationNeeded()) {
                optionToBeEvaluated = currentChoiceOption;
                // Relabeling case for concolic execution
                assert isConcolic;
                if (!solverManager.isSatisfiable()) {
                    optionToBeEvaluated.setUnsatisfiable();
                    return Optional.empty();
                } else {
                    optionToBeEvaluated.setSatisfiable();
                }
            } else {
                Optional<Choice.ChoiceOption> optionalChoiceOption = this.choiceOptionDequeRetriever.apply(deque);
                if (optionalChoiceOption.isEmpty()) {
                    return Optional.empty();
                }
                optionToBeEvaluated = optionalChoiceOption.get();
                assert !optionToBeEvaluated.isUnsatisfiable();
                adjustSolverManagerToNewChoiceOption(optionToBeEvaluated);
                if (!checkIfSatisfiableAndSet(optionToBeEvaluated)) {
                    return Optional.empty();
                }
            }
            assert currentChoiceOption.getDepth() == (solverManager.getLevel() - 1); // SolverManager has level starting at 1 after pushing initial TRUE
            return Optional.of(new SymbolicExecution(
                    this,
                    choicePointFactory,
                    valueFactory,
                    calculationFactory,
                    optionToBeEvaluated,
                    prototypicalExecutionBudgetManager,
                    mulibValueTransformer.getNextSarrayId(),
                    config
            ));
        } catch (Throwable t) {
            t.printStackTrace();
            throw new MulibRuntimeException(t);
        }
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
