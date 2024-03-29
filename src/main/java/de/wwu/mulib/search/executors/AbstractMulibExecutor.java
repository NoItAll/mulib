package de.wwu.mulib.search.executors;

import de.wwu.mulib.Fail;
import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.And;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.Not;
import de.wwu.mulib.constraints.PartnerClassObjectConstraint;
import de.wwu.mulib.expressions.ConcolicMathematicalContainer;
import de.wwu.mulib.search.budget.ExecutionBudgetManager;
import de.wwu.mulib.search.choice_points.Backtrack;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.ChoiceOptionDeque;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.solving.ArrayInformation;
import de.wwu.mulib.solving.PartnerClassObjectInformation;
import de.wwu.mulib.solving.Solution;
import de.wwu.mulib.solving.Solvers;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.ValueFactory;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Snumber;
import de.wwu.mulib.substitutions.primitives.Sprimitive;
import de.wwu.mulib.throwables.ExceededBudget;
import de.wwu.mulib.throwables.MulibException;
import de.wwu.mulib.throwables.MulibRuntimeException;
import de.wwu.mulib.transformations.MulibValueTransformer;
import de.wwu.mulib.util.TriConsumer;

import java.lang.invoke.MethodHandle;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Supertype for mulib executors. Implements the template pattern so that subclasses can focus on providing search strategies
 * as specified in {@link SearchStrategy}.
 */
public abstract class AbstractMulibExecutor implements MulibExecutor {
    /**
     * The instance of {@link SymbolicExecution} currently used to execute the search region
     */
    protected SymbolicExecution currentSymbolicExecution;
    /**
     * Stores the root choice of the search tree
     */
    protected final Choice rootChoiceOfSearchTree;
    /**
     * Stores the currently targeted choice option. This is not in sync with the choice option of SymbolicExecution
     * until the last ChoiceOption of the known path is reached
     */
    protected Choice.ChoiceOption currentChoiceOption;
    // Statistics
    protected long heuristicSatEvals = 0, satEvals = 0, unsatEvals = 0,
            addedAfterBacktrackingPoint = 0, solverBacktrack = 0;
    /**
     * Stores the {@link MulibExecutorManager} managing this {@link AbstractMulibExecutor}.
     */
    protected final MulibExecutorManager mulibExecutorManager;
    /**
     * Stores true if this instance has terminated and cannot be sensibly called anymore
     */
    protected boolean paused = false;
    /**
     * Stores the solver manager exclusive to this mulib executor
     */
    protected final SolverManager solverManager;
    /**
     * Stores the chosen search strategy
     */
    protected final SearchStrategy searchStrategy;
    private final ExecutionBudgetManager prototypicalExecutionBudgetManager;
    private final MulibValueTransformer mulibValueTransformer;
    private final MulibConfig config;
    private final TriConsumer<MulibExecutor, PathSolution, SolverManager> pathSolutionCallback;
    private final TriConsumer<MulibExecutor, de.wwu.mulib.search.trees.Fail, SolverManager> failCallback;
    private final TriConsumer<MulibExecutor, de.wwu.mulib.search.trees.ExceededBudget, SolverManager> exceededBudgetCallback;
    private final TriConsumer<MulibExecutor, Backtrack, SolverManager> backtrackCallback;
    private final MethodHandle searchRegionMethod;
    private final StaticVariables staticVariables;
    private final Object[] searchRegionArgs;
    private final Map<String, Sprimitive> rememberedSprimitives;

    /**
     * Constructs a new instance
     * @param mulibExecutorManager The owning executor manager
     * @param mulibValueTransformer The value transformer used for initially transforming the arguments to search region types
     * @param config The configuration
     * @param rootChoiceOption The root of the search tree
     * @param searchStrategy The chosen search strategy
     * @param searchRegionMethod The method handle used for invoking the search region
     * @param staticVariables The instance of {@link StaticVariables} used for managing the static variables of the search region
     * @param searchRegionArgs The transformed arguments to the search region
     */
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
        this.config = config;
        this.mulibValueTransformer = mulibValueTransformer;
        this.prototypicalExecutionBudgetManager = ExecutionBudgetManager.newInstance(config);
        this.pathSolutionCallback = config.CALLBACK_PATH_SOLUTION;
        this.searchRegionMethod = searchRegionMethod;
        this.staticVariables = staticVariables.copyFromPrototype();
        this.searchRegionArgs = searchRegionArgs;
        this.rememberedSprimitives = new HashMap<>();
        this.failCallback = config.CALLBACK_FAIL;
        this.exceededBudgetCallback = config.CALLBACK_EXCEEDED_BUDGET;
        this.backtrackCallback = config.CALLBACK_BACKTRACK;
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
        paused = true;
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
    public final void addConstraintAfterBacktrackingPoint(Constraint c) {
        solverManager.addConstraintAfterNewBacktrackingPoint(c);
    }

    @Override
    public final void addNewPartnerClassObjectConstraint(PartnerClassObjectConstraint ic) {
        assert !currentSymbolicExecution.nextIsOnKnownPath();
        solverManager.addPartnerClassObjectConstraint(ic);
        currentChoiceOption.addPartnerClassConstraint(ic);
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
        while ((!getDeque().isEmpty() && !paused && !mulibExecutorManager.globalBudgetExceeded())) {
            Optional<SymbolicExecution> possibleSymbolicExecution =
                    createExecution();
            if (possibleSymbolicExecution.isPresent()) {
                SymbolicExecution symbolicExecution = possibleSymbolicExecution.get();
                if (config.SEARCH_CONCOLIC && !solverManager.isSatisfiable()) {
                    // Be skeptical with concolic execution; - choice options might be unsatisfiable here if a labeling
                    // has become stale due to "silently" added constraints for which no choice option was created
                    currentChoiceOption.setUnsatisfiable();
                    continue;
                }
                this.currentSymbolicExecution = symbolicExecution;
                assert solverManager.isSatisfiable() : config.toString();
                try {
                    // This executes the search region with the choice path predetermined by the chosen choice option
                    Object solutionValue = invokeSearchRegion();
                    if (!solverManager.isSatisfiable()) {
                        // One last check for when the last added constraints made the constraint stack unsatisfiable.
                        // We will call solverManager.isSatisfiable() anyway to label a path solution
                        currentChoiceOption.setUnsatisfiable();
                        continue;
                    }
                    PathSolution solution;
                    try {
                        solution = getPathSolution(solutionValue, false);
                    } catch (Throwable t) {
                        throw new MulibRuntimeException(t);
                    }
                    this.mulibExecutorManager.addToPathSolutions(solution, this);
                    return Optional.of(solution);
                } catch (Backtrack b) {
                    // We assume that Backtracking is only executed in places where it is guaranteed that
                    // ChoiceOptions are not "swallowed" by backtracking, i.e.,
                    // we do not have to add back ChoiceOptions to the SearchTree's queue.
                    this.backtrackCallback.accept(this, b, solverManager);
                } catch (Fail f) {
                    de.wwu.mulib.search.trees.Fail fail = currentChoiceOption.setExplicitlyFailed();
                    this.mulibExecutorManager.addToFails(fail);
                    this.failCallback.accept(this, fail, solverManager);
                } catch (ExceededBudget be) {
                    assert !be.getExceededBudget().isIncremental() : "Should not occur anymore, we throw a normal Backtracking in this case";
                    // The newly encountered choice option triggering the exception is set in the ChoicePointFactory
                    de.wwu.mulib.search.trees.ExceededBudget exceededBudget =
                            currentChoiceOption.setBudgetExceeded(be.getExceededBudget());
                    this.mulibExecutorManager.addToExceededBudgets(exceededBudget);
                    this.exceededBudgetCallback.accept(this, exceededBudget, solverManager);
                } catch (MulibException e) {
                    if (config.SEARCH_CONCOLIC && !solverManager.isSatisfiable()) {
                        currentChoiceOption.setUnsatisfiable();
                        continue;
                    }
                    Mulib.log.warning(config.toString());
                    throw e;
                } catch (Throwable e) {
                    if (!solverManager.isSatisfiable()) {
                        // One last check for when the last added constraints made the constraint stack unsatisfiable.
                        // We will call solverManager.isSatisfiable() anyway to label a path solution if needed
                        currentChoiceOption.setUnsatisfiable();
                        continue;
                    }
                    if (config.SEARCH_ALLOW_EXCEPTIONS) {
                        PathSolution solution = getPathSolution(e, true);
                        this.mulibExecutorManager.addToPathSolutions(solution, this);
                        return Optional.of(solution);
                    } else {
                        Mulib.log.warning(config.toString());
                        throw new MulibRuntimeException("Exception was thrown but not allowed, config: " + config, e);
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void rememberSprimitive(String name, Sprimitive remembered) {
        rememberedSprimitives.put(name, ConcolicMathematicalContainer.tryGetSymFromConcolic((Snumber) remembered));
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

    // Assumes that the state of this MulibExecutor has been adapted so that a choice option has been set as
    // this.currentChoiceOption. Furthermore, this.currentSymbolicExecution must be set with this currentChoiceOption
    // as the target.
    // This method will reset all execution-specific state before copying and invoking the method handle representing
    // the search region.
    // Thereafter, it will also check whether the CoverageCfg can manifest a path.
    // Returns the return value, if the search region was left using a return-statement.
    // Returns null for void methods.
    // If the search region was left using a thrown exception, this exception, too, will be thrown
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
        if (config.FREE_INIT_ALIASING_FOR_FREE_OBJECTS) {
            // Reset aliasing information for this thread
            AliasingInformation.resetAliasingTargets();
        }
    }

    private void _manifestCfgAndReset(Object result) {
        if (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
            if (result instanceof Fail) {
                // Is explicit Fail
                getExecutorManager().getCoverageCfg().manifestTrail();
                getExecutorManager().getCoverageCfg().reset();
            } else if (!(result instanceof MulibException) && !(result instanceof Backtrack)) {
                getExecutorManager().getCoverageCfg().manifestTrail();
                // We still need to retrieve the path solution; - this is done later on
            } else {
                // Is, e.g., Backtrack
                getExecutorManager().getCoverageCfg().reset();
            }
        }
    }

    private Optional<SymbolicExecution> createExecution() {
        Choice.ChoiceOption optionToBeEvaluated;
        try {
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
            throw new MulibRuntimeException(t);
        }
    }

    @Override
    public void addExistingPartnerClassObjectConstraints(List<PartnerClassObjectConstraint> partnerClassObjectConstraints) {
        solverManager.addPartnerClassObjectConstraints(partnerClassObjectConstraints);
    }

    /**
     * Selects the next choice option from the global {@link ChoiceOptionDeque}
     * @param deque The deque storing the choice options that are candidates for evaluation
     * @return An optional wrapping a choice option, if a next choice option is picked. Else {@link Optional#empty()}.
     * Note that in a multi-threading setting with multiple {@link MulibExecutor}s, returning {@link Optional#empty()}
     * does not necessarily mean that no {@link de.wwu.mulib.search.trees.Choice.ChoiceOption} can be found anymore.
     */
    protected abstract Optional<Choice.ChoiceOption> selectNextChoiceOption(ChoiceOptionDeque deque);

    @Override
    public List<Solution> getUpToNSolutions(PathSolution searchIn, AtomicInteger N) {
        // The current constraint-representation in the constraint solver will be set to the path-solutions parent,
        // thus, in general, we must adjust the current choice option
        adjustSolverManagerToNewChoiceOption(searchIn.parentEdge);
        return solverManager.getUpToNSolutions(searchIn.getSolution(), N);
    }

    @Override
    public final boolean isSatisfiable() {
        return solverManager.isSatisfiable();
    }

    private PathSolution getPathSolution(
            Object solutionValue, // TODO Not Substituted-type since, for now, it can be of type Throwable
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

    private void backtrackOnce() {
        if (currentChoiceOption.getDepth() > 1) {
            solverManager.backtrackOnce();
            solverBacktrack++;
            currentChoiceOption = currentChoiceOption.getParentEdge();
        }
    }

    /**
     * @return The deque containing the choice options that are candidates for evaluation
     */
    protected ChoiceOptionDeque getDeque() {
        return getExecutorManager().observedTree.getChoiceOptionDeque();
    }

    private ChoicePointFactory getChoicePointFactory() {
        return getExecutorManager().choicePointFactory;
    }

    private ValueFactory getValueFactory() {
        return getExecutorManager().valueFactory;
    }

    private CalculationFactory getCalculationFactory() {
        return getExecutorManager().calculationFactory;
    }
    private void adjustSolverManagerToNewChoiceOption(final Choice.ChoiceOption optionToBeEvaluated) {
        // Backtrack with solver's push- and pop-capabilities
        final Choice.ChoiceOption backtrackTo = SearchTree.getDeepestSharedAncestor(optionToBeEvaluated, currentChoiceOption);
        int depthDifference = (currentChoiceOption.getDepth() - backtrackTo.getDepth());
        solverManager.backtrack(depthDifference);
        solverBacktrack += depthDifference;
        ArrayDeque<Choice.ChoiceOption> getPathBetween = SearchTree.getPathBetween(backtrackTo, optionToBeEvaluated);
        for (Choice.ChoiceOption co : getPathBetween) {
            solverManager.addConstraintAfterNewBacktrackingPoint(co.getOptionConstraint());
            solverManager.addPartnerClassObjectConstraints(co.getPartnerClassObjectConstraints());
            addedAfterBacktrackingPoint++;
        }
        currentChoiceOption = optionToBeEvaluated.isEvaluated() ? optionToBeEvaluated : optionToBeEvaluated.getParentEdge();
    }

    private Choice.ChoiceOption takeChoiceOptionFromNextAlternatives(List<Choice.ChoiceOption> options) {
        if (config.SEARCH_RANDOMIZE_SELECTION_FROM_NEW_CHOICE) {
            options = new ArrayList<>(options);
            Collections.shuffle(options);
        }
        for (Choice.ChoiceOption choiceOption : options) {
            if (checkIfSatisfiableAndSet(choiceOption)) {
                return choiceOption;
            }
        }
        return null;
    }

    private boolean checkIfSatisfiableAndSet(Choice.ChoiceOption choiceOption) {
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
        if (!config.SEARCH_CONCOLIC // Exclude concolic execution; - the solver manager might be unsatisfiable here
                && choiceOption.getChoice().getChoiceOptions().size() == 2
                && other.isUnsatisfiable()
                && choiceOption.getParentEdge().isEvaluated()
                && !choiceOption.getParentEdge().constraintWasModifiedAfterInitialSatCheck()
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

    private boolean checkSatWithSolver(SolverManager solverManager, Choice.ChoiceOption choiceOption) {
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
    public Optional<Choice.ChoiceOption> decideOnNextChoiceOptionDuringExecution(List<Choice.ChoiceOption> options) {
        Choice.ChoiceOption result = null;
        ExecutionBudgetManager ebm = currentSymbolicExecution.getExecutionBudgetManager();
        boolean isActualIncrementalBudgetExceeded =
                ebm.incrementalActualChoicePointBudgetIsExceeded();
        if (shouldContinueExecution()) {
            result = takeChoiceOptionFromNextAlternatives(options);
        }
        if (paused || result == null || isActualIncrementalBudgetExceeded) {
            backtrackOnce();
            // Optional.empty() means backtracking. Is used in ChoicePointFactory.
            return Optional.empty();
        } else {
            return Optional.of(result);
        }
    }

    /**
     * @return Determines whether we should continue evaluating choice options in the current execution using
     * {@link SymbolicExecution}.
     */
    protected abstract boolean shouldContinueExecution();

    @Override
    public Object getStaticField(String fieldName) {
        assert config.TRANSF_TRANSFORMATION_REQUIRED : "Static variables are only supported if we transform the search region";
        return staticVariables.getStaticField(fieldName);
    }

    @Override
    public void setStaticField(String fieldName, Object value) {
        assert config.TRANSF_TRANSFORMATION_REQUIRED : "Static variables are only supported if we transform the search region";
        staticVariables.setStaticField(fieldName, value);
    }

    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public List<Solution> reenableForMoreSolutions(AtomicInteger N) {
        paused = false;
        if (!solverManager.mustUseOtherPathSolutionForMoreSolutions() // There still potentially are solutions on this path
                && !mulibExecutorManager.globalBudgetExceeded()) {
            assert this.currentChoiceOption.getChild() instanceof PathSolution;
            List<Solution> solutions = this.solverManager.getUpToNSolutions(((PathSolution) currentChoiceOption.getChild()).getSolution(), N);
            return solutions;
        }
        return Collections.emptyList();
    }

    @Override
    public StaticVariables getStaticVariables() {
        return staticVariables;
    }
}
