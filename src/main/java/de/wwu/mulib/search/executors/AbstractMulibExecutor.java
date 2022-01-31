package de.wwu.mulib.search.executors;

import de.wwu.mulib.Fail;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.And;
import de.wwu.mulib.constraints.ArrayConstraint;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.MulibException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.search.ExceededBudget;
import de.wwu.mulib.search.choice_points.Backtrack;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.ChoiceOptionDeque;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.solving.LabelUtility;
import de.wwu.mulib.solving.Labels;
import de.wwu.mulib.solving.Solvers;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.Conc;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public abstract class AbstractMulibExecutor implements MulibExecutor {
    protected SymbolicExecution currentSymbolicExecution;
    protected final Choice rootChoiceOfSearchTree;
    // Gets the currently targeted choice option. This is not in sync with the choice option of SymbolicExecution
    // until the last ChoiceOption of the known path is reached
    protected Choice.ChoiceOption currentChoiceOption;
    protected long heuristicSatEvals = 0;
    protected long satEvals = 0;
    protected long unsatEvals = 0;
    protected long addedAfterBacktrackingPoint = 0;
    protected long solverBacktrack = 0;
    protected final MulibExecutorManager mulibExecutorManager;
    protected final SolverManager solverManager;
    protected final SearchStrategy searchStrategy;
    private final boolean labelResultValue;
    protected boolean terminated = false;
    private final boolean isConcolic;

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
    public final void setTerminated(boolean terminated) {
        this.terminated = terminated;
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
            return solverManager.getLabel((Sprimitive) var);
        } else {
            throw new NotYetImplementedException();
        }
    }

    @Override
    public final MulibExecutorManager getExecutorManager() {
        return mulibExecutorManager;
    }

    @Override
    public final Choice.ChoiceOption getCurrentChoiceOption() {
        return currentChoiceOption;
    }

    @Override
    public List<PathSolution> runForSolutions() {
        Optional<PathSolution> possibleSolution = runForSingleSolution();
        return possibleSolution.map(Collections::singletonList).orElse(Collections.emptyList());
    }

    @Override
    public Object concretize (SubstitutedVar var) {
        if (var instanceof Conc) {
            if (var instanceof Sint.ConcSint) {
                return ((Sint.ConcSint) var).intVal();
            } else if (var instanceof Sdouble.ConcSdouble) {
                return ((Sdouble.ConcSdouble) var).doubleVal();
            } else if (var instanceof Sfloat.ConcSfloat) {
                return ((Sfloat.ConcSfloat) var).floatVal();
            } else {
                throw new NotYetImplementedException();
            }
        } else if (var instanceof Sprimitive) {
            // TODO add constraint
            return solverManager.getLabel((Sprimitive) var);
        } else {
            return currentSymbolicExecution.getMulibValueTransformer().labelValue(var, solverManager);
        }
    }

    @Override
    public Optional<PathSolution> runForSingleSolution() {
        while (!getDeque().isEmpty() && !terminated) {
            Optional<SymbolicExecution> possibleSymbolicExecution =
                    createExecution(getDeque(), getChoicePointFactory(), getValueFactory(), getCalculationFactory());
            if (possibleSymbolicExecution.isPresent()) {
                SymbolicExecution symbolicExecution = possibleSymbolicExecution.get();
                try {
                    assert solverManager.isSatisfiable();
                    Object solutionValue = invokeSearchRegion();
                    if (!solverManager.isSatisfiable()) {
                        throw new Fail();
                    }
                    PathSolution solution;
                    try {
                        solution = generateSolution(solutionValue, symbolicExecution, false);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        throw new MulibRuntimeException(t);
                    }
                    this.mulibExecutorManager.addToPathSolutions(solution);
                    return Optional.of(solution);
                } catch (Backtrack b) {
                    // We assume that Backtracking is only executed in a ChoicePointFactory.
                    // In the ChoicePointFactory, ChoiceOptions are not "swallowed" by backtracking, i.e.,
                    // we do not have to add back ChoiceOptions to the SearchTree's queue, this is taken care of
                    // in the ChoicePointFactory.
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
                    throw e;
                } catch (Exception | AssertionError e) {
                    PathSolution solution = generateSolution(e, symbolicExecution, true);
                    return Optional.of(solution);
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw new MulibRuntimeException(t);
                }
            }
        }
        return Optional.empty();
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

    protected PathSolution generateSolution(
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
                symbolicExecution.getMulibValueTransformer(),
                symbolicExecution.getNamedVariables()
        );
        PathSolution solution;
        if (labelResultValue) {
            if (solutionValue != null && solutionValue.getClass().isArray()) {
                solutionValue = symbolicExecution.getMulibValueTransformer().labelValue(solutionValue, solverManager);
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
                    solverManager.getArrayConstraints()
            );
        } else {
            solution = currentChoiceOption.setSolution(
                    solutionValue,
                    labels,
                    solverManager.getConstraints().toArray(new Constraint[0]),
                    solverManager.getArrayConstraints()
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
}
