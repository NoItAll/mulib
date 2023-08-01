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
import de.wwu.mulib.solving.ArrayInformation;
import de.wwu.mulib.solving.PartnerClassObjectInformation;
import de.wwu.mulib.solving.Solvers;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.MulibValueCopier;
import de.wwu.mulib.transformations.MulibValueTransformer;
import de.wwu.mulib.util.TriConsumer;

import java.lang.invoke.MethodHandle;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractMulibExecutor implements MulibExecutor {
    protected SymbolicExecution currentSymbolicExecution;
    protected final Choice rootChoiceOfSearchTree;
    // Gets the currently targeted choice option. This is not in sync with the choice option of SymbolicExecution
    // until the last ChoiceOption of the known path is reached
    protected Choice.ChoiceOption currentChoiceOption;
    // Statistics
    protected long heuristicSatEvals = 0, satEvals = 0, unsatEvals = 0,
            addedAfterBacktrackingPoint = 0, solverBacktrack = 0;
    // Manager
    protected final MulibExecutorManager mulibExecutorManager;
    protected boolean terminated = false;
    // Executor-specific state
    protected final SolverManager solverManager;
    // Config
    protected final SearchStrategy searchStrategy;
    protected final boolean isConcolic;
    private final ExecutionBudgetManager prototypicalExecutionBudgetManager;
    private final MulibValueTransformer mulibValueTransformer;
    private final MulibConfig config;
    private final TriConsumer<MulibExecutor, PathSolution, SolverManager> pathSolutionCallback;
    private final MethodHandle searchRegionMethod;
    private final StaticVariables staticVariables;
    private final Object[] searchRegionArgs;
    private final Map<String, Sprimitive> rememberedSprimitives;

    public AbstractMulibExecutor(
            MulibExecutorManager mulibExecutorManager,
            MulibValueTransformer mulibValueTransformer,
            MulibConfig config,
            Choice.ChoiceOption rootChoiceOption,
            SearchStrategy searchStrategy,
            MethodHandle searchRegionMethod,
            StaticVariables staticVariables,
            Object[] searchRegionArgs) {
        this.currentChoiceOption = rootChoiceOption; // Is mutable and will be adapted throughout search
        this.rootChoiceOfSearchTree = rootChoiceOption.getChoice();
        this.mulibExecutorManager = mulibExecutorManager;
        this.solverManager = Solvers.getSolverManager(config);
        this.searchStrategy = searchStrategy;
        this.isConcolic = config.CONCOLIC;
        this.config = config;
        this.mulibValueTransformer = mulibValueTransformer;
        this.prototypicalExecutionBudgetManager = ExecutionBudgetManager.newInstance(config);
        this.pathSolutionCallback = config.PATH_SOLUTION_CALLBACK;
        this.searchRegionMethod = searchRegionMethod;
        this.staticVariables = staticVariables.copyFromPrototype();
        this.searchRegionArgs = searchRegionArgs;
        this.rememberedSprimitives = new HashMap<>();
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
    public PartnerClassObjectInformation getAvailableInformationOnPartnerClassObject(Sint id, String field, int depth) {
        return solverManager.getAvailableInformationOnPartnerClassObject(id, field, depth);
    }

    @Override
    public ArrayInformation getAvailableInformationOnArray(Sint id, int depth) {
        return solverManager.getAvailableInformationOnArray(id, depth);
    }

    @Override
    public final void terminate() {
        terminated = true;
        solverManager.shutdown();
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
    public final void addExistingPartnerClassObjectConstraints(List<PartnerClassObjectConstraint> ics) {
        solverManager.addPartnerClassObjectConstraints(ics);
    }

    @Override
    public final void addNewPartnerClassObjectConstraint(PartnerClassObjectConstraint ic) {
        assert !currentSymbolicExecution.nextIsOnKnownPath();
        solverManager.addPartnerClassObjectConstraint(ic);
        currentChoiceOption.addPartnerClassConstraintConstraint(ic);
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
                SymbolicExecution symbolicExecution = possibleSymbolicExecution.get();
                this.currentSymbolicExecution = symbolicExecution;
                assert solverManager.isSatisfiable() : config.toString();
                try {
                    // This executes the search region with the choice path predetermined by the chosen choice option
                    Object solutionValue = invokeSearchRegion();
                    if (!solverManager.isSatisfiable()) {
                        currentChoiceOption.setUnsatisfiable();
                        continue;
                    }
                    PathSolution solution;
                    try {
                        solution = getPathSolution(solutionValue, false);
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
                    Mulib.log.warning(config.toString());
                    throw e;
                } catch (Throwable e) {
                    if (isConcolic && !solverManager.isSatisfiable()) {
                        currentChoiceOption.setUnsatisfiable();
                        continue;
                    }
                    if (config.ALLOW_EXCEPTIONS) {
                        PathSolution solution = getPathSolution(e, true);
                        this.mulibExecutorManager.addToPathSolutions(solution, this);
                        return Optional.of(solution);
                    } else {
                        Mulib.log.warning(config.toString());
                        e.printStackTrace();
                        throw new MulibRuntimeException("Exception was thrown but not expected, config: " + config, e);
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void remember(String name, SubstitutedVar remembered) {
        if (remembered == null) { // TODO Should we remember explicit nulls?
            return;
        }
        if (remembered instanceof PartnerClass) {
            PartnerClass pc = (PartnerClass) remembered;
            pc.__mulib__setIsNamed();
            // TODO Another remember-method should take a whole set of SubstitutedVars with their names to remember
            //  The benefit would be that they all recognize object identity as they come from the same MulibValueCopier
            MulibValueCopier mulibValueCopier = new MulibValueCopier(currentSymbolicExecution, config);
            boolean isToBeLazilyInitializedButIsInsteadRepresentedSymbolically = false;
            if (pc.__mulib__isToBeLazilyInitialized()) {
                isToBeLazilyInitializedButIsInsteadRepresentedSymbolically = true;
                pc.__mulib__prepareToRepresentSymbolically(currentSymbolicExecution);
                getCalculationFactory().representPartnerClassObjectIfNeeded(currentSymbolicExecution, pc, null, null, null);
            }
            PartnerClass copied = (PartnerClass) pc.copy(mulibValueCopier);
            if (!currentSymbolicExecution.nextIsOnKnownPath()) {
                PartnerClassObjectConstraint rememberConstraint = new PartnerClassObjectRememberConstraint(
                        name,
                        copied,
                        isToBeLazilyInitializedButIsInsteadRepresentedSymbolically
                );
                solverManager.addPartnerClassObjectConstraint(rememberConstraint);
                currentChoiceOption.addPartnerClassConstraintConstraint(rememberConstraint);
            }
        } else {
            assert remembered instanceof Snumber;
            rememberedSprimitives.put(name, ConcolicNumericContainer.tryGetSymFromConcolic((Snumber) remembered));
        }
    }

    private static Object[] copyArguments(
            Object[] searchRegionArgs,
            MulibValueCopier copier) {
        Map<Object, Object> replacedMap = new IdentityHashMap<>();
        Object[] arguments = new Object[searchRegionArgs.length];
        for (int i = 0; i < searchRegionArgs.length; i++) {
            Object arg = searchRegionArgs[i];
            Object newArg;
            if ((newArg = replacedMap.get(arg)) != null) {
                arguments[i] = newArg;
                continue;
            }
            if (arg instanceof Sprimitive) {
                newArg = copier.copySprimitive((Sprimitive) arg);
            } else {
                // Is null, Sarray, PartnerClass, or should have custom copying behavior
                newArg = copier.copyNonSprimitive(arg);
            }
            replacedMap.put(arg, newArg);
            arguments[i] = newArg;
        }
        return arguments;
    }

    private Object invokeSearchRegion() throws Throwable {
        MulibValueCopier mulibValueCopier = new MulibValueCopier(currentSymbolicExecution, config);
        staticVariables.setMulibValueCopier(mulibValueCopier);
        rememberedSprimitives.clear();
        try {
            solverManager.resetLabels();
            Object result;
            if (searchRegionArgs.length == 0) {
                result = searchRegionMethod.invoke();
            } else {
                Object[] args = copyArguments(searchRegionArgs, mulibValueCopier);
                result = searchRegionMethod.invokeWithArguments(args);
            }
            _resetExecutionSpecificState();
            _manifestCfgAndReset(result);
            return result;
        } catch (Throwable t) {
            _resetExecutionSpecificState();
            _manifestCfgAndReset(t);
            throw t;
        }
    }

    private void _resetExecutionSpecificState() {
        // Reset static variables for this thread
        staticVariables.reset();
        // Remove symbolic execution for this thread;
        SymbolicExecution.remove();
        if (config.ALIASING_FOR_FREE_OBJECTS) {
            // Reset aliasing information for this thread
            AliasingInformation.resetAliasingTargets();
        }
    }

    private void _manifestCfgAndReset(Object result) {
        if (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
            if (!(result instanceof MulibException)) {
                getExecutorManager().getCoverageCfg().manifestTrail();
                // We still need to retrieve the path solution; - this is done later on
            } else if (result instanceof Fail) {
                // Is explicit Fail
                getExecutorManager().getCoverageCfg().manifestTrail();
                getExecutorManager().getCoverageCfg().reset();
            } else {
                // Is, e.g., Backtrack
                getExecutorManager().getCoverageCfg().reset();
            }
        }
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
                    mulibValueTransformer.getNextPartnerClassObjectNr(),
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

    private PathSolution getPathSolution(
            Object solutionValue, // TODO Not SubstitutedVar since, for now, it can be of type Throwable
            boolean isThrownException) {
        Solution s = solverManager.labelSolution(solutionValue, rememberedSprimitives);
        SearchTree.AccumulatedChoiceOptionConstraints constraintContainer = SearchTree.getAllConstraintsForChoiceOption(currentChoiceOption);
        PathSolution result;
        if (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
            BitSet cover = mulibExecutorManager.getCoverageCfg().getCoverAndReset();
            if (isThrownException) {
                result = currentChoiceOption.setExceptionSolution(
                        s,
                        constraintContainer.constraints,
                        constraintContainer.partnerClassObjectConstraints,
                        cover
                );
            } else {
                result = currentChoiceOption.setSolution(
                        s,
                        constraintContainer.constraints,
                        constraintContainer.partnerClassObjectConstraints,
                        cover
                );
            }
        } else {
            if (isThrownException) {
                result = currentChoiceOption.setExceptionSolution(
                        s,
                        constraintContainer.constraints,
                        constraintContainer.partnerClassObjectConstraints
                );
            } else {
                result = currentChoiceOption.setSolution(
                        s,
                        constraintContainer.constraints,
                        constraintContainer.partnerClassObjectConstraints
                );
            }
        }

        pathSolutionCallback.accept(this, result, solverManager);
        return result;
    }

    private void _addAfterBacktrackingPoint(Choice.ChoiceOption choiceOption) {
        assert currentChoiceOption != choiceOption;
        assert choiceOption.getPartnerClassObjectConstraints().isEmpty();
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
    protected void adjustSolverManagerToNewChoiceOption(final Choice.ChoiceOption optionToBeEvaluated) {
        // Backtrack with solver's push- and pop-capabilities
        final Choice.ChoiceOption backtrackTo = SearchTree.getDeepestSharedAncestor(optionToBeEvaluated, currentChoiceOption);
        int depthDifference = (currentChoiceOption.getDepth() - backtrackTo.getDepth());
        solverManager.backtrack(depthDifference);
        solverBacktrack += depthDifference;
        ArrayDeque<Choice.ChoiceOption> getPathBetween = SearchTree.getPathBetween(backtrackTo, optionToBeEvaluated);
        for (Choice.ChoiceOption co : getPathBetween) {
            solverManager.addConstraintAfterNewBacktrackingPoint(co.getOptionConstraint());
            addExistingPartnerClassObjectConstraints(co.getPartnerClassObjectConstraints());
            addedAfterBacktrackingPoint++;
        }
        currentChoiceOption = optionToBeEvaluated.isEvaluated() ? optionToBeEvaluated : optionToBeEvaluated.getParent();
    }

    protected Choice.ChoiceOption takeChoiceOptionFromNextAlternatives(List<Choice.ChoiceOption> options) {
        for (Choice.ChoiceOption choiceOption : options) {
            if (checkIfSatisfiableAndSet(choiceOption)) {
                return choiceOption;
            }
        }
        return null;
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
                && other.getPartnerClassObjectConstraints().isEmpty()) {
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

    @Override
    public Optional<Choice.ChoiceOption> chooseNextChoiceOption(List<Choice.ChoiceOption> options) {
        Choice.ChoiceOption result = null;
        ExecutionBudgetManager ebm = currentSymbolicExecution.getExecutionBudgetManager();
        boolean isActualIncrementalBudgetExceeded =
                ebm.incrementalActualChoicePointBudgetIsExceeded();
        if (shouldContinueExecution()) {
            result = takeChoiceOptionFromNextAlternatives(options);
        }
        if (terminated || result == null || isActualIncrementalBudgetExceeded) {
            backtrackOnce();
            // Optional.empty() means backtracking. Is used in ChoicePointFactory.
            return Optional.empty();
        } else {
            return Optional.of(result);
        }
    }

    protected abstract boolean shouldContinueExecution();

    @Override
    public Object getStaticField(String fieldName) {
        return staticVariables.getStaticField(fieldName);
    }

    @Override
    public void setStaticField(String fieldName, Object value) {
        staticVariables.setStaticField(fieldName, value);
    }
}
