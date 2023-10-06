package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectConstraint;
import de.wwu.mulib.solving.Solution;
import de.wwu.mulib.solving.ArrayInformation;
import de.wwu.mulib.solving.PartnerClassObjectInformation;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interface for a manager of a constraint solver. The constraint solver does not need to be incremental, but this interface
 * for the largest part assumes incremental-like behavior.
 * Here, constraints are added and the satisfiability of a constraint stack is calculated.
 * Furthermore, this is also the interface to label values with.
 * An example of how a non-incremental (in terms of backtracking points) constraint solver might behave can be found in
 * {@link Z3GlobalLearningSolverManager}.
 */
public interface SolverManager {

    /**
     * Adds a constraint to the current constraint stack
     * @param constraint The constraint to add
     */
    void addConstraint(Constraint constraint);

    /**
     * Adds a constraint after creating a new point to backtrack to.
     * A backtracking point forms a scope in which constraints are added.
     * Backtracking to such a backtracking point means that all constraints after this backtracking points
     * are effectively removed from the constraint stack
     * @param constraint The constraint
     */
    void addConstraintAfterNewBacktrackingPoint(Constraint constraint);

    /**
     * Adds a new partner class object constraint. This constraint too is subject to backtracking
     * @param ac The partner class object constraint
     */
    void addPartnerClassObjectConstraint(PartnerClassObjectConstraint ac);

    /**
     * Adds a list of partner class object constraints to the scope of the current backtracking point
     * @param acs The constraints
     */
    void addPartnerClassObjectConstraints(List<PartnerClassObjectConstraint> acs);

    /**
     * Checks whether the current constraint stack is satisfiable if the new constraint c would be added to it.
     * The constraint c is not effectively enforced thereafter.
     * @param c The constraint
     * @return true, if the constraint stack would be satisfiable, else false
     */
    boolean checkWithNewConstraint(Constraint c);

    /**
     * @return true, if the current constraint stack is satisfiable, else false
     */
    boolean isSatisfiable();

    /**
     * Removes all constraints added after the latest pushed backtracking point as well as
     * the backtracking point
     */
    void backtrackOnce();

    /**
     * Effectively removes as many backtracking points and the respective constraints as the specified number
     * @param numberOfChoiceOptions The number of backtracking operations
     */
    void backtrack(int numberOfChoiceOptions);

    /**
     * Effectively removes all constraints and backtracking points
     */
    void backtrackAll();

    /**
     * Resets the current set of (search region representation, label)-pairs
     */
    void resetLabels();

    /**
     * Transforms the search space-representation of an object into the original representation useable by a usual
     * Java program.
     * @param var The to-be-transformed Object. The type 'SubstitutedVar' is not used here, since, e.g., ArrayLists might also
     *            be labeled with a custom labeling strategy.
     * @return The labeled object.
     */
    Object getLabel(Object var);

    /**
     * Constructs a solution object comprising the returnValue and the remembered primitives. The solution object will
     * also contain labels for all remembered partner class object constraints
     * @param returnValue The return value
     * @param rememberedSprimitives The remembered primitives
     * @return The solution object
     */
    // TODO Get rid of Map<String, Sprimitive> rememberedSprimitives parameter
    // TODO returnValue should be of type SubstitutedVar. For now, it is Object until we model Throwable
    Solution labelSolution(Object returnValue, Map<String, Sprimitive> rememberedSprimitives);

    /**
     * @return The current level of the solver. Is equivalent to the depth in the search tree
     */
    int getLevel();

    /**
     * Given an initial solution, tries to construct N more solutions in the same constraint system
     * @param initialSolution The initial solution
     * @param N The number of solutions to be retrieved
     * @return The retrieved solutions
     */
    List<Solution> getUpToNSolutions(Solution initialSolution, AtomicInteger N);

    /**
     * @return True, if there potentially are more solutions, i.e., valid labelings on the current path solution, else false
     */
    boolean mustUseOtherPathSolutionForMoreSolutions();

    /**
     * Returns metadata information on a non-sarray partner class object and the content of its field
     * @param id The identifier of the non-sarray partner class object
     * @param field The field in the scheme packageName.className.fieldName
     * @param depth The depth for which to retrieve the information
     * @return A container comprising metadata information
     */
    PartnerClassObjectInformation getAvailableInformationOnPartnerClassObject(Sint id, String field, int depth);

    /**
     * Returns metadata information on a array object and the content of its field
     * @param id The identifier of the sarray object
     * @param depth The depth for which to retrieve the information
     * @return A container comprising metadata information
     */
    ArrayInformation getAvailableInformationOnArray(Sint id, int depth);

    /**
     * Terminates all resources of this constraint solver
     */
    void shutdown();

    /**
     * Registers a (search region representation, label)-pair
     * @param toLabel The search region representation object
     * @param label The labeled value
     */
    void registerLabelPair(Object toLabel, Object label);
}
