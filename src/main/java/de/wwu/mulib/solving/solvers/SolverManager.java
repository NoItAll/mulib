package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectConstraint;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.solving.ArrayInformation;
import de.wwu.mulib.solving.PartnerClassObjectInformation;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public interface SolverManager {

    void addConstraint(Constraint constraint);

    void addConstraintAfterNewBacktrackingPoint(Constraint constraint);

    void addPartnerClassObjectConstraint(PartnerClassObjectConstraint ac);

    void addPartnerClassObjectConstraints(List<PartnerClassObjectConstraint> acs);

    boolean checkWithNewConstraint(Constraint c);

    boolean isSatisfiable();

    void backtrackOnce();

    void backtrack(int numberOfChoiceOptions);

    void backtrackAll();

    void resetLabels();

    /**
     * Transforms the search space-representation of an object into the original representation useable by a usual
     * Java program.
     * @param var The to-be-transformed Object. The type 'SubstitutedVar' is not used here, since, e.g., ArrayLists might also
     *            be labeled with a custom labeling strategy.
     * @return The labeled object.
     */
    Object getLabel(Object var);

    // TODO Get rid of Map<String, Sprimitive> rememberedSprimitives parameter
    // TODO returnValue should be of type SubstitutedVar. For now, it is Object until we model Throwable
    Solution labelSolution(Object returnValue, Map<String, Sprimitive> rememberedSprimitives);

    ArrayDeque<Constraint> getConstraints();

    List<PartnerClassObjectConstraint> getAllPartnerClassObjectConstraints();

    int getLevel();

    List<Solution> getUpToNSolutions(Solution initialSolution, AtomicInteger N);

    PartnerClassObjectInformation getAvailableInformationOnPartnerClassObject(Sint id, String field, int depth);

    ArrayInformation getAvailableInformationOnArray(Sint id, int depth);

    void shutdown();

    void registerLabelPair(Object toLabel, Object label);
}
