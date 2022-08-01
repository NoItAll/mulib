package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.Not;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.budget.ExecutionBudgetManager;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.ChoiceOptionDeque;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.ValueFactory;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class GenericExecutor extends AbstractMulibExecutor {
    private final Function<ChoiceOptionDeque, Optional<Choice.ChoiceOption>> choiceOptionDequeRetriever;
    private final boolean continueExecution;
    private final ExecutionBudgetManager prototypicalExecutionBudgetManager;
    private long dsasMissed;
    private final MulibValueTransformer prototypicalMulibValueTransformer;

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
        this.prototypicalMulibValueTransformer = mulibValueTransformer;
    }

    @Override
    public Optional<Choice.ChoiceOption> chooseNextChoiceOption(List<Choice.ChoiceOption> options) {
        Choice.ChoiceOption result = null;
        ExecutionBudgetManager ebm = currentSymbolicExecution.getExecutionBudgetManager();
        boolean isActualIncrementalBudgetExceeded =
                ebm.incrementalActualChoicePointBudgetIsExceeded();
        for (Choice.ChoiceOption choiceOption : options) {
            if (continueExecution && checkIfSatisfiableAndSet(choiceOption)) {
                result = choiceOption;
                break;
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
            while ((!terminated && !deque.isEmpty() && !mulibExecutorManager.globalBudgetExceeded()) || currentChoiceOption.reevaluationNeeded()) {
                if (currentChoiceOption.reevaluationNeeded()) {
                    optionToBeEvaluated = currentChoiceOption;
                    // Relabeling case for concolic execution
                    assert isConcolic;
                    if (!solverManager.isSatisfiable()) {
                        assert solverManager.getTemporaryAssumptions().isEmpty();
                        optionToBeEvaluated.setUnsatisfiable();
                        continue;
                    }
                    optionToBeEvaluated.setSatisfiable();
                } else {
                    Optional<Choice.ChoiceOption> optionalChoiceOption = this.choiceOptionDequeRetriever.apply(deque);
                    if (optionalChoiceOption.isEmpty()) {
                        continue;
                    }
                    optionToBeEvaluated = optionalChoiceOption.get();
                    assert !optionToBeEvaluated.isUnsatisfiable();
                    adjustSolverManagerToNewChoiceOption(optionToBeEvaluated);
                    if (!checkIfSatisfiableAndSet(optionToBeEvaluated)) {
                        continue;
                    }
                }
                assert currentChoiceOption.getDepth() == (solverManager.getLevel() - 1);
                return Optional.of(new SymbolicExecution(
                        this,
                        choicePointFactory,
                        valueFactory,
                        calculationFactory,
                        optionToBeEvaluated,
                        prototypicalExecutionBudgetManager,
                        prototypicalMulibValueTransformer
                ));
            }
            return Optional.empty();
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

    private boolean checkIfSatisfiableAndSet(Choice.ChoiceOption choiceOption) {
        assert !choiceOption.isEvaluated();
        assert !choiceOption.isBudgetExceeded();
        assert !choiceOption.isUnsatisfiable();
        assert !choiceOption.isCutOff();
        assert !choiceOption.isExplicitlyFailed();
        assert currentChoiceOption == null ||
                (currentChoiceOption.getChild() instanceof Choice
                        && ((Choice) currentChoiceOption.getChild()).getChoiceOptions().stream()
                        .anyMatch(co -> choiceOption == co));
        if (choiceOption.isSatisfiable()) {
            addAfterBacktrackingPoint(choiceOption);
            return true;
        } else if (choiceOption.isUnsatisfiable()) {
            return false;
        }

        int otherNumber = choiceOption.choiceOptionNumber == 0 ? 1 : 0;
        if (choiceOption.getChoice().getChoiceOptions().size() == 2
                && choiceOption.getChoice().getOption(otherNumber).isUnsatisfiable()
                && choiceOption.getParent().isEvaluated()) {
            // If the first choice option is not satisfiable, the choice is binary, and the parent
            // is satisfiable, then the other choice option must be satisfiable, assuming that it is the negation
            // of the first choice.
            Choice.ChoiceOption other = choiceOption.getChoice().getOption(otherNumber);
            assert other != choiceOption;
            Constraint c0 = other.getOptionConstraint();
            Constraint c1 = choiceOption.getOptionConstraint();
            if ((c1 instanceof Not && ((Not) c1).isNegationOf(c0))
                    || (c0 instanceof Not && ((Not) c0).isNegationOf(c1))) {
                choiceOption.setSatisfiable();
                choiceOption.setOptionConstraint(Sbool.ConcSbool.TRUE);
                addAfterBacktrackingPoint(choiceOption);
                heuristicSatEvals++;
                assert solverManager.isSatisfiable();
                return true;
            }
        }

        addAfterBacktrackingPoint(choiceOption);
        return checkSatWithSolver(solverManager, choiceOption);
    }

    private boolean checkSatWithSolver(SolverManager solverManager, Choice.ChoiceOption choiceOption) {
        if (solverManager.isSatisfiable()) {
            choiceOption.setSatisfiable();
            satEvals++;
            return true;
        } else {
            solverManager.resetTemporaryAssumptions();
            // If there are no temporary assumptions, the AbstractIncrementalEnabledSolverManager will used the cached result,
            // i.e., no costly sat-check is executed
            if (solverManager.isSatisfiable()) {
                choiceOption.setSatisfiable();
                satEvals++;
                return true;
            }
            choiceOption.setUnsatisfiable();
            unsatEvals++;
            // Needed during chooseNextChoiceOption(Choice) for when one is unsatisfiable. This way
            // calling isSatisfiable(ChoiceOption) always leaves the MulibExecutor on a satisfiable
            // ChoiceOption.
            backtrackOnce();
            return false;
        }
    }

    private void adjustSolverManagerToNewChoiceOption(Choice.ChoiceOption optionToBeEvaluated) {
        // Backtrack with solver's push- and pop-capabilities
        Choice.ChoiceOption backtrackTo = SearchTree.getDeepestSharedAncestor(optionToBeEvaluated, currentChoiceOption);
        int depthDifference = (currentChoiceOption.getDepth() - backtrackTo.getDepth());
        solverManager.backtrack(depthDifference);
        solverBacktrack += depthDifference;
        ArrayDeque<Choice.ChoiceOption> getPathBetween = SearchTree.getPathBetween(backtrackTo, optionToBeEvaluated);
        for (Choice.ChoiceOption co : getPathBetween) {
            solverManager.addConstraintAfterNewBacktrackingPoint(co.getOptionConstraint());
            addExistingArrayConstraints(co.getArrayConstraints());
            addedAfterBacktrackingPoint++;
        }
        currentChoiceOption = optionToBeEvaluated.getParent();
    }
}
