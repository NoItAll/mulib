package de.wwu.mulib.search.executors;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectConstraint;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.solving.ArrayInformation;
import de.wwu.mulib.solving.PartnerClassObjectInformation;
import de.wwu.mulib.solving.Solution;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents an execution strategy that is constructing instances of {@link SymbolicExecution} to execute the search region with.
 * It is the middle man between the execution of the search region using {@link SymbolicExecution} to find
 * {@link PathSolution}s and the {@link MulibExecutorManager} starting the execution.
 * It has two tasks:
 * 1) Decide on the next choice option to evaluate
 * 2) Connect its constructed instances of {@link SymbolicExecution} to the constraint solver and other context relevant for
 * the execution. Other context includes: (1) Forwarding calls of {@link SymbolicExecution} to get or set static variables to
 * {@link StaticVariables}, ensuring that each {@link SymbolicExecution} starts with the same view on the static variables,
 * (2) resetting the {@link AliasingInformation} build by the former {@link SymbolicExecution} so that the latter instances
 * can start from a clean slate and (3) keeping track of covered branches using {@link de.wwu.mulib.search.choice_points.CoverageCfg}.
 * Note that this list might be extended in the future.
 */
public interface MulibExecutor {

    /**
     * Adds a list of partner class object constraints that are already attached to a {@link de.wwu.mulib.search.trees.Choice.ChoiceOption}
     * to the constraint solver.
     */
    void addExistingPartnerClassObjectConstraints(List<PartnerClassObjectConstraint> partnerClassObjectConstraints);


    /**
     * If there are any available and satisfiable {@link de.wwu.mulib.search.trees.Choice.ChoiceOption}:
     * Constructs a new instance of {@link SymbolicExecution} to evaluate the search region and tries to find a
     * {@link PathSolution}.
     * @return If a {@link PathSolution} can be retrieved, this is returned in an instance of {@link Optional}.
     * Otherwise {@link Optional#empty()} is returned.
     * Note that in a multi-threading setting with multiple {@link MulibExecutor}s, returning {@link Optional#empty()}
     * does not necessarily mean that no {@link PathSolution} can be found anymore.
     */
    Optional<PathSolution> getPathSolution();

    /**
     * Returns up to N solutions that reside on a {@link PathSolution}.
     * @param searchIn The path solution on which, aside from the initial solution, other solutions using a different
     *                 labeling might be found.
     * @param N The maximum number of path solutions to retrieve.
     * @return A list of solutions that could be found for this one path solution
     */
    List<Solution> getUpToNSolutions(PathSolution searchIn, AtomicInteger N);

    /**
     * @return Informative statistics on the execution
     */
    LinkedHashMap<String, String> getStatistics();

    /**
     * @return The {@link MulibExecutorManager} responsible for managing this instance of {@link MulibExecutor}.
     */
    MulibExecutorManager getExecutorManager();

    /**
     * @return The enum value describing the chosen search strategy.
     */
    SearchStrategy getSearchStrategy();

    /**
     * Notifies the {@link MulibExecutorManager} about new choice options encountered at a certain depth.
     * @param depth The depth
     * @param choiceOptions The choice options
     */
    void notifyNewChoice(int depth, List<Choice.ChoiceOption> choiceOptions);

    /**
     * During the ongoing execution with an instance of {@link SymbolicExecution}, decides on whether and which
     * {@link de.wwu.mulib.search.trees.Choice.ChoiceOption} is chosen to be executed next.
     * Choosing no {@link de.wwu.mulib.search.trees.Choice.ChoiceOption} means that we backtrack and will start a new
     * execution choosing an unevaluated {@link de.wwu.mulib.search.trees.Choice.ChoiceOption}.
     * The chosen constraint, as given by {@link Choice.ChoiceOption#getOptionConstraint()}, will be pushed to the
     * {@link SolverManager} after a new backtracking point.
     * @param options The options from which to choose
     * @return A {@link de.wwu.mulib.search.trees.Choice.ChoiceOption}, if the current execution using {@link SymbolicExecution}
     * should continue according to the search strategy. Otherwise {@link Optional#empty()}
     */
    Optional<Choice.ChoiceOption> decideOnNextChoiceOptionDuringExecution(List<Choice.ChoiceOption> options);

    /**
     * Labels the value, i.e., contacts the constraint solver to assign a value to it
     * @param var The value that should be labeled
     * @return A valid label
     */
    Object label(Object var);

    /**
     * Labels the value and restricts the domain of the value to this label
     * @param substitutedVar The value
     * @return A valid label and the only label that will be valid for the value hereinafter
     */
    Object concretize(Object substitutedVar);

    /**
     * Closes any resources used. The MulibExecutor is not sensibly callable after calling this method.
     */
    void terminate();

    /**
     * Pauses the MulibExecutor but do not terminate it yet.
     */
    void pause();

    /**
     * Reenable a paused MulibExecutor, finds more solutions.
     * Is used for {@link MulibExecutorManager#getUpToNSolutions(int, boolean)} if the boolean parameter is set to false.
     * @return A list with solutions that are still present on the current path.
     */
    List<Solution> reenableForMoreSolutions(AtomicInteger N);

    /**
     * Contacts the constraint solver to check if the current constraint stack in conjunction with the
     * new constraint is satisfiable. This method DOES NOT enforce the constraint on the constraint stack, i.e.,
     * without adding the constraint, subsequent calls to {@link MulibExecutor#isSatisfiable()} will not assert that
     * the constraint holds.
     * @param c The constraint
     * @return True if the constraint stack in conjunction with the new constraint is satisfiable.
     */
    boolean checkWithNewConstraint(Constraint c);

    /**
     * Adds a new constraint to the constraint solver. Furthermore, attaches this constraint to the current
     * {@link de.wwu.mulib.search.trees.Choice.ChoiceOption}.
     * When adding a constraint in the search region, calls to this method via
     * {@link SymbolicExecution#addNewConstraint(Constraint)} should always be guarded via
     * !{@link SymbolicExecution#nextIsOnKnownPath()}. Otherwise, constraints are added redundantly to already
     * evaluated {@link de.wwu.mulib.search.trees.Choice.ChoiceOption}s.
     * @param c The constraint
     */
    void addNewConstraint(Constraint c);

    /**
     * Adds a new partner class constraint to the constraint solver. Furthermore, attaches this constraint to the
     * current {@link de.wwu.mulib.search.trees.Choice.ChoiceOption}.
     * When adding a constraint in the search region, calls to this method via
     * {@link SymbolicExecution#addNewConstraint(Constraint)} should always be guarded via
     * !{@link SymbolicExecution#nextIsOnKnownPath()}. Otherwise, constraints are added redundantly to already
     * evaluated {@link de.wwu.mulib.search.trees.Choice.ChoiceOption}s.
     * @param ic The partner class object constraint
     */
    void addNewPartnerClassObjectConstraint(PartnerClassObjectConstraint ic);

    /**
     * Adds a constraint to the constraint solver and signals that this constraint should be included in a new 'scope'.
     * A call to {@link SolverManager#backtrackOnce()} should remove this new constraint and, without further constraints
     * added to this scope, this constraint alone.
     * During the execution, {@link MulibExecutor#decideOnNextChoiceOptionDuringExecution(List)} should be called instead
     * to add a constraint after a new backtracking point.
     * @param c The constraint
     */
    void addConstraintAfterBacktrackingPoint(Constraint c);

    /**
     * Remembers a primitive value
     * @param name The name by which the value should be remembered
     * @param remembered The value
     */
    void rememberSprimitive(String name, Sprimitive remembered);

    /**
     * Contacts the constraint solver to determine whether the current constraint stack is satisfiable
     * @return True, if the constraint stack is satisfiable, otherwise false
     */
    boolean isSatisfiable();

    /**
     * Return information about the non-sarray partner class object represented by the specified id
     * @param id The identifier of the non-sarray partner class object
     * @param field The field name. Should follow the pattern packageName.className.fieldName
     * @param depth The depth in the search tree for which to check available information
     * @return Metainformation on the non-sarray partner class object and its field
     */
    PartnerClassObjectInformation getAvailableInformationOnPartnerClassObject(Sint id, String field, int depth);

    /**
     * Return information about the sarray object represented by the specified id
     * @param id The identifier of the sarray object
     * @param depth The depth in the search tree for which to check available information
     * @return Metainformation on the non-sarray partner class object
     */
    ArrayInformation getAvailableInformationOnArray(Sint id, int depth);

    /**
     * Returns a static variable encapsulated in this {@link MulibExecutor}s {@link StaticVariables} instance.
     * @param fieldName The name of the static variable. Should follow the pattern packageName.className.fieldName
     * @return The content of the static field
     */
    Object getStaticField(String fieldName);

    /**
     * Sets a static variable encapsulated in this {@link MulibExecutor}s {@link StaticVariables} instance.
     * @param fieldName The name of the static variable. Should follow the pattern packageName.className.fieldName
     * @param value The value to set in the static field
     */
    void setStaticField(String fieldName, Object value);

    /**
     * @return The static variables used for this instance.
     */
    StaticVariables getStaticVariables();

}
