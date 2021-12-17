package de.wwu.mulib.search.executors;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.search.values.ValueFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class SingleExecutorManager extends MulibExecutorManager {
    public SingleExecutorManager(
            SearchTree observedTree,
            ChoicePointFactory choicePointFactory,
            ValueFactory valueFactory,
            MulibConfig config) {
        super(
                config,
                new ArrayList<>(),
                observedTree,
                choicePointFactory,
                valueFactory
        );
    }

    @Override
    public Optional<PathSolution> getSolution() {
        globalExecutionManagerBudgetManager.resetTimeBudget();
        MulibExecutor mulibExecutor = mulibExecutors.get(0);
        while (!observedTree.getChoiceOptionDeque().isEmpty() && !globalBudgetExceeded()) {
            Optional<PathSolution> possibleSymbolicExecution = mulibExecutor.runForSingleSolution();
            if (possibleSymbolicExecution.isPresent()) {
                return possibleSymbolicExecution;
            }
        }
        return Optional.empty();
    }

    @Override
    public List<PathSolution> getAllSolutions() {
        globalExecutionManagerBudgetManager.resetTimeBudget();
        List<PathSolution> result = super.getAllSolutions(mulibExecutors.get(0));
        Mulib.log.log(Level.INFO, mulibExecutors.get(0).getStatistics().toString());
        return result;
    }

    @Override
    public List<PathSolution> getAllSolutions(List<Object> args) {
        globalExecutionManagerBudgetManager.resetTimeBudget();
        List<PathSolution> result = super.getAllSolutions(mulibExecutors.get(0), args);
        Mulib.log.log(Level.INFO, mulibExecutors.get(0).getStatistics().toString());
        return result;
    }
}
