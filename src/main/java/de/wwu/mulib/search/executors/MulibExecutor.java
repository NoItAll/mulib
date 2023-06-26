package de.wwu.mulib.search.executors;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectConstraint;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.solving.ArrayInformation;
import de.wwu.mulib.solving.PartnerClassObjectInformation;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public interface MulibExecutor {

    void addExistingPartnerClassObjectConstraints(List<PartnerClassObjectConstraint> acs);

    Optional<PathSolution> getPathSolution();

    List<Solution> getUpToNSolutions(PathSolution searchIn, AtomicInteger N);

    LinkedHashMap<String, String> getStatistics();

    MulibExecutorManager getExecutorManager();

    SearchStrategy getSearchStrategy();

    void notifyNewChoice(int depth, List<Choice.ChoiceOption> choiceOptions);

    Optional<Choice.ChoiceOption> chooseNextChoiceOption(List<Choice.ChoiceOption> options);

    Object label(Object var);

    Object concretize(Object substitutedVar);

    /**
     * Closes any resources used. The MulibExecutor is not sensibly callable anymore thereafter.
     */
    void terminate();

    boolean checkWithNewConstraint(Constraint c);

    void addNewConstraint(Constraint c);

    void addNewPartnerClassObjectConstraint(PartnerClassObjectConstraint ic);

    void addNewConstraintAfterBacktrackingPoint(Constraint c);

    void remember(String name, SubstitutedVar remembered);

    boolean isSatisfiable();

    PartnerClassObjectInformation getAvailableInformationOnPartnerClassObject(Sint id, String field);

    ArrayInformation getAvailableInformationOnArray(Sint id);

    Object getStaticField(String fieldName);

    void setStaticField(String fieldName, Object value);

}
