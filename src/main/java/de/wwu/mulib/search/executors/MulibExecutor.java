package de.wwu.mulib.search.executors;

import de.wwu.mulib.constraints.ArrayConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.substitutions.SubstitutedVar;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public interface MulibExecutor {

    void addExistingArrayConstraints(List<ArrayConstraint> acs);

    Optional<PathSolution> runForSingleSolution();

    List<PathSolution> runForSolutions();

    LinkedHashMap<String, String> getStatistics();

    MulibExecutorManager getExecutorManager();

    SearchStrategy getSearchStrategy();

    Choice.ChoiceOption getCurrentChoiceOption();

    Optional<Choice.ChoiceOption> chooseNextChoiceOption(List<Choice.ChoiceOption> options);

    Object label(SubstitutedVar var);

    Object concretize(SubstitutedVar substitutedVar);

    void setTerminated(boolean terminated);

    boolean checkWithNewConstraint(Constraint c);

    void addNewConstraint(Constraint c);

    void addNewArrayConstraint(ArrayConstraint ac);

    void addTemporaryAssumption(Constraint c);

    void resetTemporaryAssumptions();

    boolean checkWithNewArrayConstraint(ArrayConstraint ac);

    void addNewConstraintAfterBacktrackingPoint(Constraint c);

    boolean isSatisfiable();
}
