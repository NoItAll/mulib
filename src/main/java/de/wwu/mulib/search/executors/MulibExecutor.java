package de.wwu.mulib.search.executors;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.IdentityHavingSubstitutedVarConstraint;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.solving.IdentityHavingSubstitutedVarInformation;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public interface MulibExecutor {

    void addExistingIdentityHavingSubstitutedVarConstraints(List<IdentityHavingSubstitutedVarConstraint> acs);

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

    void addNewIdentitiyHavingSubstitutedVarConstraint(IdentityHavingSubstitutedVarConstraint ic);

    void addNewConstraintAfterBacktrackingPoint(Constraint c);

    boolean isSatisfiable();

    IdentityHavingSubstitutedVarInformation getAvailableInformationOnIdentityHavingSubstitutedVar(Sint id);
}
