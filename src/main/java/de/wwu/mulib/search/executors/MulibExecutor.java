package de.wwu.mulib.search.executors;

import de.wwu.mulib.constraints.ArrayConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public interface MulibExecutor {

    void addExistingArrayConstraints(List<ArrayConstraint> acs);

    Optional<PathSolution> getPathSolution();

    List<Solution> getUpToNSolutions(PathSolution searchIn, AtomicInteger N);

    LinkedHashMap<String, String> getStatistics();

    MulibExecutorManager getExecutorManager();

    SearchStrategy getSearchStrategy();

    void notifyNewChoice(int depth, List<Choice.ChoiceOption> choiceOptions);

    Optional<Choice.ChoiceOption> chooseNextChoiceOption(List<Choice.ChoiceOption> options);

    Object label(Object var);

    Object concretize(Object substitutedVar);

    void terminate();

    boolean checkWithNewConstraint(Constraint c);

    void addNewConstraint(Constraint c);

    void addNewArrayConstraint(ArrayConstraint ac);

    boolean checkWithNewArrayConstraint(ArrayConstraint ac);

    void addNewConstraintAfterBacktrackingPoint(Constraint c);

    boolean isSatisfiable();
}
