package de.wwu.mulib.search.executors;

import de.wwu.mulib.Fail;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.Not;
import de.wwu.mulib.exceptions.MulibException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.ExceededBudget;
import de.wwu.mulib.search.budget.ExecutionBudgetManager;
import de.wwu.mulib.search.choice_points.Backtrack;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.ChoiceOptionDeque;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.search.values.ValueFactory;
import de.wwu.mulib.solving.Labels;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;

public final class GenericExecutor extends MulibExecutor {
    private final Function<ChoiceOptionDeque, Optional<Choice.ChoiceOption>> choiceOptionDequeRetriever;
    private final boolean continueExecution;
    private final ExecutionBudgetManager prototypicalExecutionBudgetManager;
    private final Choice rootChoiceOfSearchTree;
    private long dsasMissed;
    private final boolean labelResultValue;

    public GenericExecutor(
            Choice.ChoiceOption rootChoiceOption,
            MulibExecutorManager mulibExecutorManager,
            MulibConfig config,
            SearchStrategy searchStrategy) {
        super(mulibExecutorManager, config, searchStrategy);
        this.currentChoiceOption = rootChoiceOption; // Is mutable and will be adapted throughout search
        this.rootChoiceOfSearchTree = rootChoiceOption.getChoice();
        this.labelResultValue = config.LABEL_RESULT_VALUE;
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
    }

    @Override
    public LinkedHashMap<String, String> getStatistics() {
        LinkedHashMap<String, String> result = super.getStatistics();
        if (searchStrategy == SearchStrategy.DSAS) {
            result.put("missedDsas", String.valueOf(dsasMissed));
        }
        return result;
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

    private ChoiceOptionDeque getDeque() {
        return mulibExecutorManager.observedTree.getChoiceOptionDeque();
    }

    private ChoicePointFactory getChoicePointFactory() {
        return mulibExecutorManager.choicePointFactory;
    }

    private ValueFactory getValueFactory() {
        return mulibExecutorManager.valueFactory;
    }

    private MethodHandle getMethodHandle() {
        return mulibExecutorManager.observedTree.representedMethod;
    }

    @Override
    public Optional<PathSolution> runForSingleSolution() {
        while (!getDeque().isEmpty() && !terminated) {
            Optional<SymbolicExecution> possibleSymbolicExecution =
                    createExecution(getDeque(), getChoicePointFactory(), getValueFactory());
            if (possibleSymbolicExecution.isPresent()) {
                SymbolicExecution symbolicExecution = possibleSymbolicExecution.get();
                try {
                    Object solutionValue = getMethodHandle().invoke();
                    PathSolution solution;
                    try {
                        solution = generateSolution(solutionValue, symbolicExecution, false);
                    } catch (Throwable t) {
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
                } catch (Exception e) {
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

    private PathSolution generateSolution(
            Object solutionValue,
            SymbolicExecution symbolicExecution,
            boolean isThrownException) {
        Choice.ChoiceOption choiceOption = symbolicExecution.getCurrentChoiceOption();
        if (solutionValue != null && labelResultValue && !solutionValue.getClass().isArray() && solutionValue instanceof SubstitutedVar) {
            symbolicExecution.addTrackedVariable("return", solutionValue);
        }
        Labels labels = solverManager.getLabels(symbolicExecution.getTrackedVariables());
        PathSolution solution;
        if (isThrownException) {
            solution = choiceOption.setExceptionSolution(
                    (Throwable) solutionValue,
                    labels,
                    solverManager.getConstraints().toArray(new Constraint[0])
            );
        } else {
            if (labelResultValue) {
                if (solutionValue != null && solutionValue.getClass().isArray()) {
                    solutionValue = transformArray(solutionValue, labels);
                } else if (solutionValue instanceof SubstitutedVar) {
                    solutionValue = transformOneValue((SubstitutedVar) solutionValue, labels);
                }
            }
            solution = choiceOption.setSolution(
                    solutionValue,
                    labels,
                    solverManager.getConstraints().toArray(new Constraint[0])
            );
        }
        return solution;
    }

    private Object transformArray(Object o, Labels l) {
        Class<?> componentType = o.getClass().getComponentType();
        int length = Array.getLength(o);
        Object[] result = new Object[length];
        if (componentType.isArray()) {
            for (int i = 0; i < length; i++) {
                result[i] = transformArray(Array.get(o, i), l);
            }
        } else {
            for (int i = 0; i < length; i++) {
                Object val = Array.get(o, i);
                if (val instanceof SubstitutedVar) {
                    result[i] = transformOneValue((SubstitutedVar) val, l);
                } else {
                    return o;
                }
            }
        }
        return result;
    }

    private Object transformOneValue(SubstitutedVar o, Labels l) {
        if (o instanceof Sint) {
            if (o instanceof Sint.ConcSint) {
                return ((Sint.ConcSint) o).intVal();
            } else if (o instanceof Sint.SymSint) {
                return l.getForTrackedSubstitutedVar(o);
            } else {
                throw new NotYetImplementedException();
            }
        } else if (o instanceof Sdouble) {
            if (o instanceof Sdouble.ConcSdouble) {
                return ((Sdouble.ConcSdouble) o).doubleVal();
            } else if (o instanceof Sdouble.SymSdouble) {
                return l.getForTrackedSubstitutedVar(o);
            } else {
                throw new NotYetImplementedException();
            }
        } else if (o instanceof Sfloat) {
            if (o instanceof Sfloat.ConcSfloat) {
                return ((Sfloat.ConcSfloat) o).floatVal();
            } else if (o instanceof Sfloat.SymSfloat) {
                return l.getForTrackedSubstitutedVar(o);
            } else {
                throw new NotYetImplementedException();
            }
        } else if (o instanceof Slong) {
            if (o instanceof Slong.ConcSlong) {
                return ((Slong.ConcSlong) o).longVal();
            } else if (o instanceof Slong.SymSlong) {
                return l.getForTrackedSubstitutedVar(o);
            } else {
                throw new NotYetImplementedException();
            }
        } else if (o instanceof Sshort) {
            if (o instanceof Sshort.ConcSshort) {
                return ((Sshort.ConcSshort) o).shortVal();
            } else if (o instanceof Sshort.SymSshort) {
                return l.getForTrackedSubstitutedVar(o);
            } else {
                throw new NotYetImplementedException();
            }
        } else if (o instanceof Sbyte) {
            if (o instanceof Sbyte.ConcSbyte) {
                return ((Sbyte.ConcSbyte) o).byteVal();
            } else if (o instanceof Sbyte.SymSbyte) {
                return l.getForTrackedSubstitutedVar(o);
            } else {
                throw new NotYetImplementedException();
            }
        } else if (o instanceof Sbool) {
            if (o instanceof Sbool.ConcSbool) {
                return ((Sbool.ConcSbool) o).isTrue();
            } else if (o instanceof Sbool.SymSbool) {
                return l.getForTrackedSubstitutedVar(o);
            } else {
                throw new NotYetImplementedException();
            }
        } else {
            throw new NotYetImplementedException();
        }
    }

    @Override
    public List<PathSolution> runForSolutions() {
        Optional<PathSolution> possibleSolution = runForSingleSolution();
        return possibleSolution.map(Collections::singletonList).orElse(Collections.emptyList());
    }

    private Optional<SymbolicExecution> createExecution(
            ChoiceOptionDeque deque,
            ChoicePointFactory choicePointFactory,
            ValueFactory valueFactory) {
        Choice.ChoiceOption optionToBeEvaluated;
        while (!terminated && !deque.isEmpty() && !mulibExecutorManager.globalBudgetExceeded()) {
            Optional<Choice.ChoiceOption> optionalChoiceOption = this.choiceOptionDequeRetriever.apply(deque);
            if (!optionalChoiceOption.isPresent()) { // !isPresent to be compatible with Java 8
                continue;
            }
            optionToBeEvaluated = optionalChoiceOption.get();
            assert !optionToBeEvaluated.isUnsatisfiable();
            adjustSolverManagerToNewChoiceOption(optionToBeEvaluated);
            currentChoiceOption = optionToBeEvaluated.getParent();
            if (isSatisfiable(optionToBeEvaluated)) {
                assert currentChoiceOption.getDepth() == (solverManager.getConstraints().size() - 1) :
                        "Should not occur";
                currentSymbolicExecution = new SymbolicExecution(
                        this,
                        choicePointFactory,
                        valueFactory,
                        optionToBeEvaluated,
                        prototypicalExecutionBudgetManager.copyFromPrototype()
                );
                return Optional.of(currentSymbolicExecution);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Choice.ChoiceOption> chooseNextChoiceOption(Choice choice) {
        List<Choice.ChoiceOption> options = choice.getChoiceOptions();
        Choice.ChoiceOption result = null;
        ExecutionBudgetManager ebm = currentSymbolicExecution.getExecutionBudgetManager();
        boolean isActualIncrementalBudgetExceeded =
                ebm.incrementalActualChoicePointBudgetIsExceeded();
        for (Choice.ChoiceOption choiceOption : options) {
            if (continueExecution && isSatisfiable(choiceOption)) {
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

    private boolean isSatisfiable(Choice.ChoiceOption choiceOption) {
        assert !choiceOption.isEvaluated();
        assert !choiceOption.isBudgetExceeded();
        assert !choiceOption.isUnsatisfiable();
        assert !choiceOption.isCutOff();
        assert !choiceOption.isExplicitlyFailed();
        assert currentChoiceOption == null ||
                currentChoiceOption.getChild() instanceof Choice
                        && ((Choice) currentChoiceOption.getChild()).getChoiceOptions().stream()
                        .anyMatch(co -> {
                            return choiceOption == co;
                        });
        if (choiceOption.isSatisfiable()) {
            addAfterBacktrackingPoint(choiceOption);
            return true;
        } else if (choiceOption.isUnsatisfiable()) {
            return false;
        }
        // otherNumber is only valid if there is a binary choice.
        int otherNumber = choiceOption.choiceOptionNumber == 0 ? 1 : 0;
        if (choiceOption.getChoice().getChoiceOptions().size() == 2
                && choiceOption.getChoice().getOption(otherNumber).isUnsatisfiable()
                && choiceOption.getParent().isSatisfiable()) {
            // If the first choice option is not satisfiable, the choice is binary, and the parent
            // is satisfiable, then the other choice option must be satisfiable, assuming that it is the negation
            // of the first choice.
            Choice.ChoiceOption other = choiceOption.getChoice().getOption(otherNumber);
            assert other != choiceOption;
            Constraint c0 = other.optionConstraint;
            Constraint c1 = choiceOption.optionConstraint;
            if ((c1 instanceof Not && c0 == ((Not) c1).getConstraint())
                || (c0 instanceof Not && c1 == ((Not) c0).getConstraint())) {
                choiceOption.setSatisfiable();
                heuristicSatEvals++;
                addAfterBacktrackingPoint(choiceOption);
                assert solverManager.isSatisfiable();
                return true;
            }
        }

        addAfterBacktrackingPoint(choiceOption);
        if (solverManager.isSatisfiable()) {
            choiceOption.setSatisfiable();
            satEvals++;
            return true;
        } else {
            choiceOption.setUnsatisfiable();
            unsatEvals++;
            backtrackOnce();
            return false;
        }
    }

    private void backtrackOnce() {
        if (currentChoiceOption.getDepth() > 0) {
            solverManager.backtrackOnce();
            solverBacktrack++;
            currentChoiceOption = currentChoiceOption.getParent();
        }
    }

    private void addAfterBacktrackingPoint(Choice.ChoiceOption choiceOption) {
        solverManager.addConstraintAfterNewBacktrackingPoint(choiceOption.optionConstraint);
        addedAfterBacktrackingPoint++;
        currentChoiceOption = choiceOption;
    }

    @Override
    public Object concretize (SubstitutedVar substitutedVar){
        // TODO this should also be constrained in the subsequent execution
        Object label = solverManager.getLabel(substitutedVar);
        return label;
    }

    private void adjustSolverManagerToNewChoiceOption(Choice.ChoiceOption optionToBeEvaluated){
        // Backtrack with solver's push- and pop-capabilities
        Choice.ChoiceOption backtrackTo = SearchTree.getDeepestSharedAncestor(optionToBeEvaluated, currentChoiceOption);
        int depthDifference = (currentChoiceOption.getDepth() - backtrackTo.getDepth());
        solverManager.backtrack(depthDifference);
        solverBacktrack += depthDifference;
        ArrayDeque<Choice.ChoiceOption> getPathBetween = SearchTree.getPathBetween(backtrackTo, optionToBeEvaluated);
        for (Choice.ChoiceOption co : getPathBetween) {
            solverManager.addConstraintAfterNewBacktrackingPoint(co.optionConstraint);
            addedAfterBacktrackingPoint++;
        }
    }

    // TODO Refactor
    @Override
    public List<PathSolution> runForSolutions(List<Object> arguments) {
        while (!getDeque().isEmpty() && !terminated) {
            Optional<SymbolicExecution> possibleSymbolicExecution =
                    createExecution(getDeque(), getChoicePointFactory(), getValueFactory());
            if (possibleSymbolicExecution.isPresent()) {
                SymbolicExecution symbolicExecution = possibleSymbolicExecution.get();
                try {
                    Object solutionValue = getMethodHandle().invoke();
                    PathSolution solution = generateSolution(solutionValue, symbolicExecution, false);
                    this.mulibExecutorManager.addToPathSolutions(solution);
                    return Collections.singletonList(solution);
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
                } catch (Exception e) {
                    PathSolution solution = generateSolution(e, symbolicExecution, true);
                    return Collections.singletonList(solution);
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw new MulibRuntimeException(t);
                }
            }
        }
        return Collections.emptyList();
    }

}
