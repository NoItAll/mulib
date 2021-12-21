package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.solving.Solvers;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public abstract class MulibExecutor {
    protected SymbolicExecution currentSymbolicExecution;
    protected Choice.ChoiceOption currentChoiceOption;
    protected long satEvals = 0;
    protected long unsatEvals = 0;
    protected long addedAfterBacktrackingPoint = 0;
    protected long solverBacktrack = 0;
    protected final MulibExecutorManager mulibExecutorManager;
    protected final SolverManager solverManager;
    protected final SearchStrategy searchStrategy;
    protected boolean terminated = false;

    public MulibExecutor(
            MulibExecutorManager mulibExecutorManager,
            MulibConfig config,
            SearchStrategy searchStrategy) {
        this.mulibExecutorManager = mulibExecutorManager;
        this.solverManager = Solvers.getSolverManager(config);
        this.searchStrategy = searchStrategy;
    }

    public LinkedHashMap<String, String> getStatistics() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("addedAfterBacktrackingPoint", String.valueOf(this.addedAfterBacktrackingPoint));
        result.put("satEvals", String.valueOf(this.satEvals));
        result.put("unsatEvals", String.valueOf(this.unsatEvals));
        result.put("solverBacktrack", String.valueOf(this.solverBacktrack));
        return result;
    }

    public abstract Optional<PathSolution> runForSingleSolution();

    public abstract List<PathSolution> runForSolutions();

    public final MulibExecutorManager getExecutorManager() {
        return mulibExecutorManager;
    }

    public abstract Optional<Choice.ChoiceOption> chooseNextChoiceOption(List<Choice.ChoiceOption> options);

    public abstract Object concretize(Sprimitive substitutedVar);

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    public void addNewConstraint(Constraint c) {
        solverManager.addConstraint(c);
    }
}
