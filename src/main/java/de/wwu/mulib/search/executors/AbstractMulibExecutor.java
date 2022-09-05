package de.wwu.mulib.search.executors;

import de.wwu.mulib.Fail;
import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.MulibException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.search.ExceededBudget;
import de.wwu.mulib.search.budget.ExecutionBudgetManager;
import de.wwu.mulib.search.choice_points.Backtrack;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.*;
import de.wwu.mulib.solving.LabelUtility;
import de.wwu.mulib.solving.Labels;
import de.wwu.mulib.solving.Solvers;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.SymNumericExpressionSprimitive;
import de.wwu.mulib.substitutions.primitives.ValueFactory;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public abstract class AbstractMulibExecutor implements MulibExecutor {
    protected SymbolicExecution currentSymbolicExecution;
    protected final Choice rootChoiceOfSearchTree;
    // Gets the currently targeted choice option. This is not in sync with the choice option of SymbolicExecution
    // until the last ChoiceOption of the known path is reached
    protected Choice.ChoiceOption currentChoiceOption;
    // Statistics
    protected long heuristicSatEvals = 0;
    protected long satEvals = 0;
    protected long unsatEvals = 0;
    protected long addedAfterBacktrackingPoint = 0;
    protected long solverBacktrack = 0;
    // Manager
    protected final MulibExecutorManager mulibExecutorManager;
    protected boolean terminated = false;
    // Executor-specific state
    protected final SolverManager solverManager;
    // Config
    protected final SearchStrategy searchStrategy;
    private final boolean labelResultValue;
    protected final boolean isConcolic;
    private final ExecutionBudgetManager prototypicalExecutionBudgetManager;
    private final MulibValueTransformer mulibValueTransformer;
    private final MulibConfig config;

    public AbstractMulibExecutor(
            MulibExecutorManager mulibExecutorManager,
            MulibValueTransformer mulibValueTransformer,
            MulibConfig config,
            Choice.ChoiceOption rootChoiceOption,
            SearchStrategy searchStrategy) {
        this.currentChoiceOption = rootChoiceOption; // Is mutable and will be adapted throughout search
        this.rootChoiceOfSearchTree = rootChoiceOption.getChoice();
        this.mulibExecutorManager = mulibExecutorManager;
        this.solverManager = Solvers.getSolverManager(config);
        this.searchStrategy = searchStrategy;
        this.labelResultValue = config.LABEL_RESULT_VALUE;
        this.isConcolic = config.CONCOLIC;
        this.config = config;
        this.mulibValueTransformer = mulibValueTransformer;
        this.prototypicalExecutionBudgetManager = ExecutionBudgetManager.newInstance(config);
    }

    @Override
    public final SearchStrategy getSearchStrategy() {
        return searchStrategy;
    }

    @Override
    public LinkedHashMap<String, String> getStatistics() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("addedAfterBacktrackingPoint", String.valueOf(this.addedAfterBacktrackingPoint));
        result.put("heuristicSatEvals", String.valueOf(heuristicSatEvals));
        result.put("satEvals", String.valueOf(this.satEvals));
        result.put("unsatEvals", String.valueOf(this.unsatEvals));
        result.put("solverBacktrack", String.valueOf(this.solverBacktrack));
        return result;
    }

    @Override
    public final void terminate() {
        this.terminated = true;
    }

    @Override
    public final boolean checkWithNewConstraint(Constraint c) {
        return solverManager.checkWithNewConstraint(c);
    }

    @Override
    public final void addNewConstraint(Constraint c) {
        assert !currentSymbolicExecution.nextIsOnKnownPath();
        currentChoiceOption.setOptionConstraint(
                And.newInstance(currentChoiceOption.getOptionConstraint(), c));
        solverManager.addConstraint(c);
    }

    @Override
    public final void addNewConstraintAfterBacktrackingPoint(Constraint c) {
        solverManager.addConstraintAfterNewBacktrackingPoint(c);
    }

    @Override
    public final void addExistingArrayConstraints(List<ArrayConstraint> acs) {
        solverManager.addArrayConstraints(acs);
    }

    @Override
    public final void addNewArrayConstraint(ArrayConstraint ac) {
        assert !currentSymbolicExecution.nextIsOnKnownPath();
        solverManager.addArrayConstraint(ac);
        currentChoiceOption.addArrayConstraint(ac);
    }

    @Override
    public final Object label(Object var) {
        return solverManager.getLabel(var);
    }

    @Override
    public final MulibExecutorManager getExecutorManager() {
        return mulibExecutorManager;
    }

    @Override
    public Object concretize(Object var) {
        Object result = label(var);
        // TODO add constraint
        return result;
    }

    @Override
    public void notifyNewChoice(int depth, List<Choice.ChoiceOption> choiceOptions) {
        mulibExecutorManager.notifyNewChoice(depth, choiceOptions);
    }

    @Override
    public Optional<PathSolution> getPathSolution() {
        while ((!getDeque().isEmpty() && !terminated && !mulibExecutorManager.globalBudgetExceeded()) || currentChoiceOption.reevaluationNeeded()) {
            Optional<SymbolicExecution> possibleSymbolicExecution =
                    createExecution();
            if (possibleSymbolicExecution.isPresent()) {
                solverManager.setupForNewExecution();
                SymbolicExecution symbolicExecution = possibleSymbolicExecution.get();
                this.currentSymbolicExecution = symbolicExecution;
                assert solverManager.isSatisfiable() : config.toString();
                try {
                    // This executes the search region with the choice path predetermined by the chosen choice option
                    Object solutionValue = invokeSearchRegion();
                    if (!solverManager.isSatisfiable()) {
                        throw new Fail();
                    }
                    PathSolution solution;
                    try {
                        solution = getSolution(solutionValue, symbolicExecution, false);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        throw new MulibRuntimeException(t);
                    }
                    this.mulibExecutorManager.addToPathSolutions(solution, this);
                    return Optional.of(solution);
                } catch (Backtrack b) {
                    // We assume that Backtracking is only executed in places where it is guaranteed that
                    // ChoiceOptions are not "swallowed" by backtracking, i.e.,
                    // we do not have to add back ChoiceOptions to the SearchTree's queue.
                } catch (Fail f) {
                    de.wwu.mulib.search.trees.Fail fail = symbolicExecution.getCurrentChoiceOption().setExplicitlyFailed();
                    this.mulibExecutorManager.addToFails(fail);
                } catch (ExceededBudget be) {
                    assert !be.getExceededBudget().isIncremental() : "Should not occur anymore, we throw a normal Backtracking in this case";
                    // The newly encountered choice option triggering the exception is set in the ChoicePointFactory
                    de.wwu.mulib.search.trees.ExceededBudget exceededBudget =
                            symbolicExecution.getCurrentChoiceOption().setBudgetExceeded(be.getExceededBudget());
                    this.mulibExecutorManager.addToExceededBudgets(exceededBudget);
                } catch (MulibException e) {
                    Mulib.log.log(Level.WARNING, config.toString());
                    throw e;
                } catch (Exception | AssertionError e) {
                    if (config.ALLOW_EXCEPTIONS) {
                        PathSolution solution = getSolution(e, symbolicExecution, true);
                        this.mulibExecutorManager.addToPathSolutions(solution, this);
                        return Optional.of(solution);
                    } else {
                        Mulib.log.log(Level.WARNING, config.toString());
                        throw new MulibRuntimeException("Exception was thrown but not expected, config: " + config, e);
                    }
                } catch (Throwable t) {
                    Mulib.log.log(Level.WARNING, config.toString());
                    throw new MulibRuntimeException(t);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<SymbolicExecution> createExecution() {
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
                Optional<Choice.ChoiceOption> optionalChoiceOption = selectNextChoiceOption(getDeque());
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
            assert currentChoiceOption.getDepth() == solverManager.getLevel();
            return Optional.of(new SymbolicExecution(
                    this,
                    getChoicePointFactory(),
                    getValueFactory(),
                    getCalculationFactory(),
                    optionToBeEvaluated,
                    prototypicalExecutionBudgetManager,
                    mulibValueTransformer.getNextSymSarrayId(),
                    config
            ));
        } catch (Throwable t) {
            t.printStackTrace();
            throw new MulibRuntimeException(t);
        }
    }

    protected abstract Optional<Choice.ChoiceOption> selectNextChoiceOption(ChoiceOptionDeque deque);

    @Override
    public List<Solution> getUpToNSolutions(PathSolution searchIn, AtomicInteger N) {
        // The current constraint-representation in the constraint solver will be set to the path-solutions parent,
        // thus, in general, we must adjust the current choice option
        adjustSolverManagerToNewChoiceOption(searchIn.parent);
        return solverManager.getUpToNSolutions(searchIn.getSolution(), N);
    }

    @Override
    public final boolean isSatisfiable() {
        return solverManager.isSatisfiable();
    }

    private PathSolution getSolution(
            Object solutionValue,
            SymbolicExecution symbolicExecution,
            boolean isThrownException) {
        if (solutionValue instanceof SubstitutedVar
                && labelResultValue) {
            symbolicExecution.addNamedVariable("return", (SubstitutedVar) solutionValue);
        }
        Labels labels = LabelUtility.getLabels(
                solverManager,
                symbolicExecution.getNamedVariables()
        );
        PathSolution solution;
        if (labelResultValue) {
            if (solutionValue != null && solutionValue.getClass().isArray()) {
                solutionValue = solverManager.getLabel(solutionValue);
            } else if (solutionValue instanceof SubstitutedVar) {
                solutionValue = labels.getLabelForNamedSubstitutedVar((SubstitutedVar) solutionValue);
            }
        } else {
            if (isConcolic && solutionValue instanceof Sbool.SymSbool) {
                if (((Sbool.SymSbool) solutionValue).getRepresentedConstraint() instanceof ConcolicConstraintContainer) {
                    solutionValue = ((ConcolicConstraintContainer) ((Sbool.SymSbool) solutionValue).getRepresentedConstraint()).getSym();
                }
            } else if (isConcolic && solutionValue instanceof SymNumericExpressionSprimitive
                    && ((SymNumericExpressionSprimitive) solutionValue).getRepresentedExpression() instanceof ConcolicNumericContainer) {
                solutionValue = ((ConcolicNumericContainer) ((SymNumericExpressionSprimitive) solutionValue).getRepresentedExpression()).getSym();
            }
        }

        if (isThrownException) {
            solution = currentChoiceOption.setExceptionSolution(
                    (Throwable) solutionValue,
                    labels,
                    solverManager.getConstraints().toArray(new Constraint[0]),
                    solverManager.getArrayConstraints().toArray(new ArrayConstraint[0])
            );
        } else {
            solution = currentChoiceOption.setSolution(
                    solutionValue,
                    labels,
                    solverManager.getConstraints().toArray(new Constraint[0]),
                    solverManager.getArrayConstraints().toArray(new ArrayConstraint[0])
            );
        }
        return solution;
    }

    private void _addAfterBacktrackingPoint(Choice.ChoiceOption choiceOption) {
        assert currentChoiceOption != choiceOption;
        assert choiceOption.getArrayConstraints().isEmpty();
        solverManager.addConstraintAfterNewBacktrackingPoint(choiceOption.getOptionConstraint());
        addedAfterBacktrackingPoint++;
        currentChoiceOption = choiceOption;
    }

    protected void backtrackOnce() {
        if (currentChoiceOption.getDepth() > 1) {
            solverManager.backtrackOnce();
            solverBacktrack++;
            currentChoiceOption = currentChoiceOption.getParent();
        }
    }

    protected ChoiceOptionDeque getDeque() {
        return getExecutorManager().observedTree.getChoiceOptionDeque();
    }

    protected ChoicePointFactory getChoicePointFactory() {
        return getExecutorManager().choicePointFactory;
    }

    protected ValueFactory getValueFactory() {
        return getExecutorManager().valueFactory;
    }

    protected CalculationFactory getCalculationFactory() {
        return getExecutorManager().calculationFactory;
    }

    protected Object invokeSearchRegion() throws Throwable {
        return getExecutorManager().observedTree.invokeSearchRegion(currentSymbolicExecution);
    }

    protected void adjustSolverManagerToNewChoiceOption(final Choice.ChoiceOption optionToBeEvaluated) {
        // Backtrack with solver's push- and pop-capabilities
        final Choice.ChoiceOption backtrackTo = SearchTree.getDeepestSharedAncestor(optionToBeEvaluated, currentChoiceOption);
        int depthDifference = (currentChoiceOption.getDepth() - backtrackTo.getDepth());
        solverManager.backtrack(depthDifference);
        solverBacktrack += depthDifference;
        ArrayDeque<Choice.ChoiceOption> getPathBetween = SearchTree.getPathBetween(backtrackTo, optionToBeEvaluated);
        for (Choice.ChoiceOption co : getPathBetween) {
            solverManager.addConstraintAfterNewBacktrackingPoint(co.getOptionConstraint());
            addExistingArrayConstraints(co.getArrayConstraints());
            addedAfterBacktrackingPoint++;
        }
        currentChoiceOption = optionToBeEvaluated.isEvaluated() ? optionToBeEvaluated : optionToBeEvaluated.getParent();
    }

    protected boolean checkIfSatisfiableAndSet(Choice.ChoiceOption choiceOption) {
        assert !choiceOption.isEvaluated() && !choiceOption.isBudgetExceeded() && !choiceOption.isUnsatisfiable()
                && !choiceOption.isCutOff() && !choiceOption.isExplicitlyFailed() : choiceOption.stateToString();
        assert currentChoiceOption == null ||
                (currentChoiceOption.getChild() instanceof Choice
                        && ((Choice) currentChoiceOption.getChild()).getChoiceOptions().stream()
                        .anyMatch(co -> choiceOption == co));
        if (choiceOption.isSatisfiable()) {
            _addAfterBacktrackingPoint(choiceOption);
            return true;
        } else if (choiceOption.isUnsatisfiable()) {
            return false;
        }

        int otherNumber = choiceOption.choiceOptionNumber == 0 ? 1 : 0;
        Choice.ChoiceOption other = choiceOption.getChoice().getOption(otherNumber);
        if (choiceOption.getChoice().getChoiceOptions().size() == 2
                && other.isUnsatisfiable()
                && choiceOption.getParent().isEvaluated()
                // We want to avoid that
                && other.getArrayConstraints().isEmpty()) {
            assert solverManager.isSatisfiable();
            // If the first choice option is not satisfiable, the choice is binary, and the parent
            // is satisfiable, then the other choice option must be satisfiable, assuming that it is the negation
            // of the first choice.
            // Array constraints do not yet need to be regarded since they are only added while exploring a specific
            // choice option
            assert other != choiceOption;
            Constraint c0 = other.getOptionConstraint();
            Constraint c1 = choiceOption.getOptionConstraint();
            if ((c1 instanceof Not && ((Not) c1).isNegationOf(c0))
                    || (c0 instanceof Not && ((Not) c0).isNegationOf(c1))) {
                choiceOption.setSatisfiable();
                choiceOption.setOptionConstraint(Sbool.ConcSbool.TRUE);
                _addAfterBacktrackingPoint(choiceOption);
                heuristicSatEvals++;
                assert solverManager.isSatisfiable() : config;
                return true;
            }
        }

        _addAfterBacktrackingPoint(choiceOption);
        return checkSatWithSolver(solverManager, choiceOption);
    }

    protected boolean checkSatWithSolver(SolverManager solverManager, Choice.ChoiceOption choiceOption) {
        if (solverManager.isSatisfiable()) {
            choiceOption.setSatisfiable();
            satEvals++;
            return true;
        } else {
            choiceOption.setUnsatisfiable();
            unsatEvals++;
            // Needed during chooseNextChoiceOption(Choice) for when one is unsatisfiable. This way
            // calling isSatisfiable(ChoiceOption) always leaves the MulibExecutor on a satisfiable
            // ChoiceOption.
            backtrackOnce();
            return false;
        }
    }
}
