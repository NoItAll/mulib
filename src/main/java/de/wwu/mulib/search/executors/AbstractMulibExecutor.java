package de.wwu.mulib.search.executors;

import de.wwu.mulib.Fail;
import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.MulibException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.search.ExceededBudget;
import de.wwu.mulib.search.choice_points.Backtrack;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.ChoiceOptionDeque;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.solving.LabelUtility;
import de.wwu.mulib.solving.Labels;
import de.wwu.mulib.solving.Solvers;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sprimitive;
import de.wwu.mulib.substitutions.primitives.SymNumericExpressionSprimitive;
import de.wwu.mulib.substitutions.primitives.ValueFactory;
import de.wwu.mulib.transformations.MulibValueLabeler;

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
    private final MulibConfig config;

    public AbstractMulibExecutor(
            MulibExecutorManager mulibExecutorManager,
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
        solverManager.addConstraint(c);
        currentChoiceOption.setOptionConstraint(
                And.newInstance(currentChoiceOption.getOptionConstraint(), c));
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
        solverManager.addArrayConstraint(ac);
        currentChoiceOption.addArrayConstraint(ac);
    }


    @Override
    public final boolean checkWithNewArrayConstraint(ArrayConstraint ac) {
        return solverManager.checkWithNewArraySelectConstraint(ac);
    }

    @Override
    public final Object label(SubstitutedVar var) {
        if (var instanceof Sprimitive) {
            return currentSymbolicExecution.getMulibValueLabeler().labelSprimitive((Sprimitive) var, solverManager);
        } else {
            return currentSymbolicExecution.getMulibValueLabeler().label(var, solverManager);
        }
    }

    @Override
    public final MulibExecutorManager getExecutorManager() {
        return mulibExecutorManager;
    }

    @Override
    public Object concretize(SubstitutedVar var) {
        if (var instanceof Sprimitive) {
            // TODO add constraint
            return currentSymbolicExecution.getMulibValueLabeler().labelSprimitive((Sprimitive) var, solverManager);
        } else {
            return currentSymbolicExecution.getMulibValueLabeler().label(var, solverManager);
        }
    }

    @Override
    public Optional<PathSolution> getPathSolution() {
        while ((!getDeque().isEmpty() && !terminated && !mulibExecutorManager.globalBudgetExceeded()) || currentChoiceOption.reevaluationNeeded()) {
            Optional<SymbolicExecution> possibleSymbolicExecution =
                    createExecution(getDeque(), getChoicePointFactory(), getValueFactory(), getCalculationFactory());
            if (possibleSymbolicExecution.isPresent()) {
                SymbolicExecution symbolicExecution = possibleSymbolicExecution.get();
                this.currentSymbolicExecution = symbolicExecution;
                assert solverManager.isSatisfiable() : config.toString();
                try {
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
                    e.printStackTrace();
                    Mulib.log.log(Level.WARNING, config.toString());
                    throw e;
                } catch (Exception | AssertionError e) {
                    if (config.ALLOW_EXCEPTIONS) {
                        PathSolution solution = getSolution(e, symbolicExecution, true);
                        this.mulibExecutorManager.addToPathSolutions(solution, this);
                        return Optional.of(solution);
                    } else {
                        throw new MulibRuntimeException("Exception was thrown but not expected", e);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    Mulib.log.log(Level.WARNING, config.toString());
                    throw new MulibRuntimeException(t);
                }
            }
        }
        return Optional.empty();
    }

    protected abstract void adjustSolverManagerToNewChoiceOption(Choice.ChoiceOption adjustTo);

    @Override
    public List<Solution> getUpToNSolutions(PathSolution searchIn, AtomicInteger N) {
        // The current constraint-representation in the constraint solver will be set to the path-solutions parent,
        // thus, in general, we must adjust the current choice option
        adjustSolverManagerToNewChoiceOption(searchIn.parent);
        return solverManager.getUpToNSolutions(searchIn.getSolution(), N, new MulibValueLabeler(config, true));
    }

    @Override
    public final boolean isSatisfiable() {
        return solverManager.isSatisfiable();
    }

    protected abstract Optional<SymbolicExecution> createExecution(
            ChoiceOptionDeque deque,
            ChoicePointFactory choicePointFactory,
            ValueFactory valueFactory,
            CalculationFactory calculationFactory);

    protected PathSolution getSolution(
            Object solutionValue,
            SymbolicExecution symbolicExecution,
            boolean isThrownException) {
        if (solutionValue != null
                && labelResultValue
                && !solutionValue.getClass().isArray()
                && solutionValue instanceof SubstitutedVar) {
            symbolicExecution.addNamedVariable("return", (SubstitutedVar) solutionValue);
        }
        Labels labels = LabelUtility.getLabels(
                solverManager,
                symbolicExecution.getMulibValueLabeler(),
                symbolicExecution.getNamedVariables()
        );
        PathSolution solution;
        if (labelResultValue) {
            if (solutionValue != null && solutionValue.getClass().isArray()) {
                solutionValue = symbolicExecution.getMulibValueLabeler().label(solutionValue, solverManager);
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

    protected void addAfterBacktrackingPoint(Choice.ChoiceOption choiceOption) {
        solverManager.addConstraintAfterNewBacktrackingPoint(choiceOption.getOptionConstraint());
        addedAfterBacktrackingPoint++;
        currentChoiceOption = choiceOption;
    }

    protected void backtrackOnce() {
        if (currentChoiceOption.getDepth() > 0) {
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

    protected boolean checkIfSatisfiableAndSet(Choice.ChoiceOption choiceOption) {
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
