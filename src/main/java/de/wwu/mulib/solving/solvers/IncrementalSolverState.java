package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.*;

public class IncrementalSolverState<AR, PR> {

    // Each constraint represents one "scope" of a constraint here. That means tha a pop in a managed constraint solver
    // corresponds to a pop here.
    private final ArrayDeque<Constraint> constraints = new ArrayDeque<>();
    private int level = 0;

    // To account for mutability, free arrays are stored via a stack (arraydeque) here
    // We need to know at which level an array has gained a new representation, so that we know when to add/remove
    // a array from the Map<Sint, ArrayDeque<AR>> above
    // We also want to preserve the order in which the constraints are added!
    private final List<List<ArrayConstraint>> arrayConstraints = new ArrayList<>();
    private final List<List<PartnerClassObjectConstraint>> partnerClassObjectConstraints = new ArrayList<>();

    @SuppressWarnings("rawtypes")
    private final SymbolicPartnerClassObjectStates symbolicPartnerClassObjectStates;
    @SuppressWarnings("rawtypes")
    private IncrementalSolverState(MulibConfig config) {
        this.symbolicPartnerClassObjectStates = new SymbolicPartnerClassObjectStates(config);
    }

    public static class SymbolicPartnerClassObjectStates<R> {
        final Map<Sint, PartnerClassObjectRepresentation<R>> idToMostRecentRepresentation = new HashMap<>();
        SymbolicPartnerClassObjectStates(MulibConfig mc) {
        }

        public void addRepresentationForId(Sint id, R r, int level) {
            PartnerClassObjectRepresentation<R> pcor = new PartnerClassObjectRepresentation<>(id);
            pcor.addNewRepresentation(r, level);
            idToMostRecentRepresentation.put(id, pcor);
        }

        public PartnerClassObjectRepresentation<R> getRepresentationForId(Sint id) {
            return idToMostRecentRepresentation.get(id);
        }
    }

    @SuppressWarnings("unchecked")
    public SymbolicPartnerClassObjectStates<AR> getSymbolicArrayStates() {
        return symbolicPartnerClassObjectStates;
    }

    @SuppressWarnings("unchecked")
    public SymbolicPartnerClassObjectStates<PR> getSymbolicPartnerClassObjectStates() {
        return symbolicPartnerClassObjectStates;
    }

    void addConstraint(Constraint c) {
        // We conjoin the previous with the current constraint so that the uppermost constraint is still a valid
        // representation of the current constraint scope
        Constraint previousTop = constraints.pollFirst();
        constraints.push(And.newInstance(previousTop, c));
    }

    void pushConstraint(Constraint c) {
        constraints.push(c);
        level++;
    }

    void popConstraint() {
        // Check whether we need to update represented arrays
        popArrayConstraintForLevel();
        popPartnerClassConstraintsForLevel();
        constraints.poll();
        level--;
    }

    public AR getCurrentArrayRepresentation(Sint arrayId) {
        PartnerClassObjectRepresentation<AR> ar = _getArrayRepresentation(arrayId);
        return ar == null ? null : ar.getNewestRepresentation();
    }

    public PR getCurrentPartnerClassObjectRepresentation(Sint id) {
        PartnerClassObjectRepresentation<PR> ar = _getPartnerClassObjectRepresentation(id);
        return ar == null ? null : ar.getNewestRepresentation();
    }

    public void addArrayAccessConstraint(ArrayAccessConstraint ac) {
        assert _getArrayRepresentation(ac.getPartnerClassObjectId()) != null;
        addIdentityHavingSubstitutedVarConstraint(level, ac, arrayConstraints);
    }

    public void addPartnerClassObjectFieldAccessConstraint(PartnerClassObjectFieldConstraint pc) {
        assert _getPartnerClassObjectRepresentation(pc.getPartnerClassObjectId()) != null;
        addIdentityHavingSubstitutedVarConstraint(level, pc, partnerClassObjectConstraints);
    }

    private static <T> void addIdentityHavingSubstitutedVarConstraint(int level, T add, List<List<T>> addTo) {
        while (addTo.size() <= level) {
            addTo.add(new ArrayList<>());
        }
        addTo.get(level).add(add);
    }

    @SuppressWarnings("unchecked")
    public void initializeArrayRepresentation(ArrayInitializationConstraint constraint, AR initialRepresentation) {
        assert _getArrayRepresentation(constraint.getPartnerClassObjectId()) == null
                || _getArrayRepresentation(constraint.getPartnerClassObjectId()).getNewestRepresentation() == null : "Array was already initialized!";
        PartnerClassObjectRepresentation<AR> ar = new PartnerClassObjectRepresentation<>(constraint.getPartnerClassObjectId());
        ar.addNewRepresentation(initialRepresentation, level);
        symbolicPartnerClassObjectStates.idToMostRecentRepresentation.put(constraint.getPartnerClassObjectId(), ar);
    }

    @SuppressWarnings("unchecked")
    public void initializePartnerClassObjectRepresentation(PartnerClassObjectInitializationConstraint constraint, PR initialRepresentation) {
        assert _getPartnerClassObjectRepresentation(constraint.getPartnerClassObjectId()) == null
                || _getPartnerClassObjectRepresentation(constraint.getPartnerClassObjectId()).getNewestRepresentation() == null : "Partner class object was already initialized!";
        PartnerClassObjectRepresentation<PR> pr = new PartnerClassObjectRepresentation<>(constraint.getPartnerClassObjectId());
        pr.addNewRepresentation(initialRepresentation, level);
        symbolicPartnerClassObjectStates.idToMostRecentRepresentation.put(constraint.getPartnerClassObjectId(), pr);
    }

    public void addNewRepresentationInitializingArrayConstraint(ArrayAccessConstraint constraint, AR newRepresentation) {
        // Initialize/add new array representation
        PartnerClassObjectRepresentation<AR> ar = _getArrayRepresentation(constraint.getPartnerClassObjectId());
        assert ar != null : "Array representation was not initialized via an ArrayInitializationConstraint!";
        ar.addNewRepresentation(newRepresentation, level);
    }

    public ArrayDeque<Constraint> getConstraints() {
        return constraints;
    }

    public List<ArrayConstraint> getArrayConstraints() {
        List<ArrayConstraint> result = new ArrayList<>();
        for (List<ArrayConstraint> acs : arrayConstraints) {
            result.addAll(acs);
        }
        return result;
    }

    public List<PartnerClassObjectConstraint> getPartnerClassObjectConstraints() {
        List<PartnerClassObjectConstraint> result = new ArrayList<>();
        for (List<PartnerClassObjectConstraint> acs : partnerClassObjectConstraints) {
            result.addAll(acs);
        }
        return result;
    }

    public int getLevel() {
        return level;
    }

    @SuppressWarnings("rawtypes")
    public static IncrementalSolverState newInstance(MulibConfig config) {
        return new IncrementalSolverState(config);
    }

    private void popArrayConstraintForLevel() {
        // Check if popped level contains array constraints
        if (arrayConstraints.size() > level) {
            arrayConstraints.get(level).clear();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void popPartnerClassConstraintsForLevel() {
        // Check if popped level contains array constraints
        for (PartnerClassObjectRepresentation<?> pr :
                (Collection<PartnerClassObjectRepresentation>) symbolicPartnerClassObjectStates.idToMostRecentRepresentation.values()) {
            pr.popRepresentationsOfLevel(level);
        }
        if (partnerClassObjectConstraints.size() > level) {
            partnerClassObjectConstraints.get(level).clear();
        }
    }

    @SuppressWarnings("unchecked")
    private PartnerClassObjectRepresentation<AR> _getArrayRepresentation(Sint arrayId) {
        return (PartnerClassObjectRepresentation<AR>) symbolicPartnerClassObjectStates.idToMostRecentRepresentation.get(arrayId);
    }

    @SuppressWarnings("unchecked")
    private PartnerClassObjectRepresentation<PR> _getPartnerClassObjectRepresentation(Sint id) {
        return (PartnerClassObjectRepresentation<PR>) symbolicPartnerClassObjectStates.idToMostRecentRepresentation.get(id);
    }


    public static class PartnerClassObjectRepresentation<R> {
        // Array that is represented
        final Sint id;
        // Information for each level, including array constraints and the representation per level
        final ArrayDeque<PartnerClassObjectRepresentationForLevel<R>> representationsForLevels;
        PartnerClassObjectRepresentation(Sint id) {
            this.id = id;
            this.representationsForLevels = new ArrayDeque<>();
        }
        public R getNewestRepresentation() {
            PartnerClassObjectRepresentationForLevel<R> resultWrapper = representationsForLevels.peek();
            if (resultWrapper == null) {
                return null;
            }
            return resultWrapper.getNewestRepresentation();
        }

        public void addNewRepresentation(R newRepresentation, int level) {
            assert representationsForLevels.isEmpty() || representationsForLevels.peek().depth <= level;
            PartnerClassObjectRepresentationForLevel<R> ar = representationsForLevels.peek();
            if (ar == null || ar.depth < level) {
                representationsForLevels.push(produceRepresentationForLevel(newRepresentation, level));
            } else {
                ar.addRepresentation(newRepresentation);
            }
        }

        void popRepresentationsOfLevel(int level) {
            PartnerClassObjectRepresentationForLevel<R> arfl = representationsForLevels.peek();
            assert arfl == null || arfl.depth <= level;
            if (arfl != null && arfl.depth == level) {
                representationsForLevels.pop();
            }
            assert representationsForLevels.isEmpty() || representationsForLevels.peek().depth < level;
        }

        protected PartnerClassObjectRepresentationForLevel<R> produceRepresentationForLevel(R newRepresentation, int level) {
            return new PartnerClassObjectRepresentationForLevel<>(newRepresentation, level);
        }
    }


    private static class PartnerClassObjectRepresentationForLevel<R> {
        final int depth;
        final ArrayDeque<R> representationsOfLevel;
        PartnerClassObjectRepresentationForLevel(R representation, int depth) {
            this.representationsOfLevel = new ArrayDeque<>();
            this.representationsOfLevel.add(representation);
            this.depth = depth;
        }

        void addRepresentation(R newRepresentation) {
            this.representationsOfLevel.push(newRepresentation);
        }

        R getNewestRepresentation() {
            return this.representationsOfLevel.peek();
        }
    }
}
