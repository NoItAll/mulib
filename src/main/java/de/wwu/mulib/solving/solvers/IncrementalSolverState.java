package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.*;

/**
 * Keeps track of all constraints and their level. The usual constraints are represented by Mulib's constraints.
 * Furthermore keeps track of choosing the correct representation of objects/arrays for/in the constraint solver.
 * @param <AR> The representation of arrays
 * @param <PR> The representation of partner class objects
 */
public class IncrementalSolverState<AR, PR> {

    // Each constraint represents one "scope" of a constraint here. That means tha a pop in a managed constraint solver
    // corresponds to a pop here.
    private final ArrayDeque<Constraint> constraints = new ArrayDeque<>();
    private int level = 0;

    // To account for mutability, free arrays are stored via a stack (arraydeque) here
    // We need to know at which level an array has gained a new representation, so that we know when to add/remove
    // a array from the Map<Sint, ArrayDeque<AR>> above
    // We also want to preserve the order in which the constraints are added!
    private final List<List<PartnerClassObjectConstraint>> partnerClassObjectConstraints = new ArrayList<>();

    @SuppressWarnings("rawtypes")
    private final SymbolicPartnerClassObjectStates symbolicPartnerClassObjectStates;


    @SuppressWarnings("rawtypes")
    private IncrementalSolverState(MulibConfig config, SolverManager sm) {
        this.symbolicPartnerClassObjectStates = new SymbolicPartnerClassObjectStates<>(config, sm);
    }

    /**
     * Maintains the state of all {@link PartnerClassObjectRepresentation}s for {@link IncrementalSolverState}.
     * Can be thought of a heap of symbolic aliasing representations and aliasing target representations.
     * @param <R> The represented type of object, either AR or PR
     */
    public static class SymbolicPartnerClassObjectStates<R> {
        final SolverManager solverManager;
        final Map<Sint, PartnerClassObjectRepresentation<R>> idToMostRecentRepresentation = new HashMap<>();
        SymbolicPartnerClassObjectStates(MulibConfig mc, SolverManager sm) {
            this.solverManager = sm;
        }

        /**
         * Adds a new representation for the object with the specified identifier at the given level
         * @param id The identifier
         * @param r The representation
         * @param level The level
         */
        public void addRepresentationForId(Sint id, R r, int level) {
            PartnerClassObjectRepresentation<R> pcor = new PartnerClassObjectRepresentation<>(id);
            assert r != null;
            pcor.addNewRepresentation(r, level);
            idToMostRecentRepresentation.put(id, pcor);
        }

        /**
         * @param id The identifier of the object for which to retrieve the representation
         * @return The representation
         */
        public PartnerClassObjectRepresentation<R> getRepresentationForId(Sint id) {
            return idToMostRecentRepresentation.get(id);
        }

        /**
         * Adds a constraint to the constraint solver. This is supposed to be the metadataconstraint for
         * initializing those object, i.e., arrays or partner class objects, that are symbolic aliases of another object
         * @param metadataConstraint The metadata constraint
         */
        public void addMetadataConstraint(Constraint metadataConstraint) {
            solverManager.addConstraint(metadataConstraint);
        }

        /**
         * @return The current level of the constraint solver
         */
        public int getCurrentLevel() {
            return solverManager.getLevel();
        }

        @Override
        public String toString() {
            return String.format("SymbolicPartnerClassObjectStates{idToMostRecentRepresentation=%s}", idToMostRecentRepresentation);
        }
    }

    /**
     * Clears this instance
     */
    public void clear() {
        this.constraints.clear();
        this.partnerClassObjectConstraints.clear();
        this.allPartnerClassObjectConstraints = null;
    }

    /**
     * @return The symbolic array states containing all representations of arrays for/in the constraint solver
     */
    @SuppressWarnings("unchecked")
    public SymbolicPartnerClassObjectStates<AR> getSymbolicArrayStates() {
        return symbolicPartnerClassObjectStates;
    }

    /**
     * @return The symbolic non-array object states containing all representations of non-array objects for/in
     * the constraint solver
     */
    @SuppressWarnings("unchecked")
    public SymbolicPartnerClassObjectStates<PR> getSymbolicPartnerClassObjectStates() {
        return symbolicPartnerClassObjectStates;
    }

    /**
     * Adds a constraint to this incremental solver state; - does NOT add the constraint to the constraint solver itself.
     * The constraint is conjoined to the other constraints at the current depth
     * @param c The constraint to add
     */
    public void addConstraint(Constraint c) {
        // We conjoin the previous with the current constraint so that the uppermost constraint is still a valid
        // representation of the current constraint scope
        Constraint previousTop = constraints.pollFirst();
        constraints.push(And.newInstance(previousTop, c));
    }

    /**
     * Pushes a new constraint after a backtracking point and increments the level, i.e., a new scope for constraints
     * is opened
     * @param c The constraint
     */
    public void pushConstraint(Constraint c) {
        constraints.push(c);
        level++;
    }

    /**
     * Pops the last scope and all constraints, including {@link PartnerClassObjectConstraint}s.
     * Decrements the level
     */
    public void popConstraint() {
        // Check whether we need to update represented partner class objects
        popPartnerClassConstraintsForLevel();
        constraints.poll();
        level--;
    }

    /**
     * Returns the most recent array representation for the given identifier
     * @param arrayId The identifier
     * @return The most recent array representation. If there is no current representation, this throws an exception
     */
    public AR getCurrentArrayRepresentation(Sint arrayId) {
        PartnerClassObjectRepresentation<AR> ar = _getArrayRepresentation(arrayId);
        if (ar == null) {
            throw new MulibIllegalStateException("Must not occur");
        }
        return ar.getNewestRepresentation();
    }

    /**
     * Returns the most recent non-array object representation for the given identifier
     * @param id The identifier
     * @return The most recent representation. If there is no current representation, this throws an exception
     */
    public PR getCurrentPartnerClassObjectRepresentation(Sint id) {
        PartnerClassObjectRepresentation<PR> ar = _getPartnerClassObjectRepresentation(id);
        if (ar == null) {
            throw new MulibIllegalStateException("Must not occur");
        }
        return ar.getNewestRepresentation();
    }

    /**
     * Adds an array constraint
     * @param ac The array constraint
     */
    public void addArrayConstraint(ArrayConstraint ac) {
        assert _getArrayRepresentation(ac.getPartnerClassObjectId()) != null;
        addIdentityHavingSubstitutedVarConstraint(level, ac);
    }

    /**
     * Adds a non-array-related partner class object constraint
     * @param pc The constraint
     */
    public void addPartnerClassObjectConstraint(PartnerClassObjectConstraint pc) {
        assert pc instanceof PartnerClassObjectRememberConstraint || _getPartnerClassObjectRepresentation(pc.getPartnerClassObjectId()) != null;
        addIdentityHavingSubstitutedVarConstraint(level, pc);
    }

    private void addIdentityHavingSubstitutedVarConstraint(int level, PartnerClassObjectConstraint add) {
        while (partnerClassObjectConstraints.size() <= level) {
            partnerClassObjectConstraints.add(new ArrayList<>());
        }
        partnerClassObjectConstraints.get(level).add(add);
        allPartnerClassObjectConstraints = null;
    }

    /**
     * Initializes an array representation in the incremental solver state
     * @param constraint The constraint initializing the array
     * @param initialRepresentation The initial representation
     */
    @SuppressWarnings("unchecked")
    public void initializeArrayRepresentation(ArrayInitializationConstraint constraint, AR initialRepresentation) {
        assert _getArrayRepresentation(constraint.getPartnerClassObjectId()) == null
                || _getArrayRepresentation(constraint.getPartnerClassObjectId()).getNewestRepresentation() == null : "Array was already initialized!";
        PartnerClassObjectRepresentation<AR> ar = new PartnerClassObjectRepresentation<>(constraint.getPartnerClassObjectId());
        ar.addNewRepresentation(initialRepresentation, level);
        symbolicPartnerClassObjectStates.idToMostRecentRepresentation.put(constraint.getPartnerClassObjectId(), ar);
    }

    /**
     * Initializes an non-array representation in the incremental solver state
     * @param constraint The constraint initializing the non-array object
     * @param initialRepresentation The initial representation
     */
    @SuppressWarnings("unchecked")
    public void initializePartnerClassObjectRepresentation(PartnerClassObjectInitializationConstraint constraint, PR initialRepresentation) {
        assert _getPartnerClassObjectRepresentation(constraint.getPartnerClassObjectId()) == null
                || _getPartnerClassObjectRepresentation(constraint.getPartnerClassObjectId()).getNewestRepresentation() == null : "Partner class object was already initialized!";
        PartnerClassObjectRepresentation<PR> pr = new PartnerClassObjectRepresentation<>(constraint.getPartnerClassObjectId());
        pr.addNewRepresentation(initialRepresentation, level);
        symbolicPartnerClassObjectStates.idToMostRecentRepresentation.put(constraint.getPartnerClassObjectId(), pr);
    }

    /**
     * Adds a new representation.
     * The array must already be initialized. This new representation serves to add further constraints to and being
     * backtrackable.
     * @param constraint The constraint
     * @param newRepresentation The new representation
     */
    public void addNewRepresentationInitializingArrayConstraint(ArrayAccessConstraint constraint, AR newRepresentation) {
        // Initialize/add new array representation
        PartnerClassObjectRepresentation<AR> ar = _getArrayRepresentation(constraint.getPartnerClassObjectId());
        assert ar != null : "Array representation was not initialized via an ArrayInitializationConstraint!";
        ar.addNewRepresentation(newRepresentation, level);
    }

    /**
     * Adds a new representation.
     * The object must already be initialized. This new representation serves to add further constraints to and being
     * backtrackable.
     * @param c The constraint
     * @param newRepresentation The new representation
     */
    public void addNewRepresentationInitializingPartnerClassFieldConstraint(PartnerClassObjectFieldConstraint c, PR newRepresentation) {
        PartnerClassObjectRepresentation<PR> pr = _getPartnerClassObjectRepresentation(c.getPartnerClassObjectId());
        assert pr != null : "Partner class object representation was not initialized via a PartnerClassObjectInitializationConstraint!";
        pr.addNewRepresentation(newRepresentation, level);
    }

    /**
     * @return All constraints
     */
    public ArrayDeque<Constraint> getConstraints() {
        return constraints;
    }

    private List<PartnerClassObjectConstraint> allPartnerClassObjectConstraints = null;

    /**
     * @return All partner class object constraints, including array constraints
     */
    public List<PartnerClassObjectConstraint> getAllPartnerClassObjectConstraints() {
        if (allPartnerClassObjectConstraints != null) {
            return allPartnerClassObjectConstraints;
        }
        List<PartnerClassObjectConstraint> result = new ArrayList<>();
        for (List<PartnerClassObjectConstraint> pcocs : partnerClassObjectConstraints) {
            result.addAll(pcocs);
        }
        this.allPartnerClassObjectConstraints = result;
        return Collections.unmodifiableList(result);
    }

    /**
     * @return The current level of the constraint solver. Is equivalent to the depth in the search tree
     */
    public int getLevel() {
        return level;
    }

    /**
     * Constructs a new instance
     * @param config The config
     * @param sm The solver manager
     * @return The new instance
     */
    @SuppressWarnings("rawtypes")
    public static IncrementalSolverState newInstance(MulibConfig config, SolverManager sm) {
        return new IncrementalSolverState(config, sm);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void popPartnerClassConstraintsForLevel() {
        for (PartnerClassObjectRepresentation<?> pr :
                (Collection<PartnerClassObjectRepresentation>) symbolicPartnerClassObjectStates.idToMostRecentRepresentation.values()) {
            pr.popRepresentationsOfLevel(level);
        }
        // Check if popped level contains partner class object constraints
        if (partnerClassObjectConstraints.size() > level) {
            allPartnerClassObjectConstraints = null;
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


    /**
     * For a given object, identified by its identifier, stores all representations it has.
     * For various levels new representations might given to ensure that no side-effect constraints leak to other representations.
     * @param <R> Either AR or PR
     */
    public static class PartnerClassObjectRepresentation<R> {
        // Array that is represented
        final Sint id;
        // Information for each level, including array constraints and the representation per level
        final ArrayDeque<PartnerClassObjectRepresentationForLevel<R>> representationsForLevels;
        PartnerClassObjectRepresentation(Sint id) {
            this.id = id;
            this.representationsForLevels = new ArrayDeque<>();
        }

        /**
         * @return The most recent representation
         */
        public R getNewestRepresentation() {
            PartnerClassObjectRepresentationForLevel<R> resultWrapper = representationsForLevels.peek();
            if (resultWrapper == null) {
                return null;
            }
            return resultWrapper.getNewestRepresentation();
        }

        /**
         * @param depth The depth for which we want the most recent representation
         * @return The most recent representation of the given depth
         */
        public R getRepresentationForDepth(int depth) {
            R current = null;
            Iterator<PartnerClassObjectRepresentationForLevel<R>> it = representationsForLevels.descendingIterator();
            while (it.hasNext()) {
                PartnerClassObjectRepresentationForLevel<R> r = it.next();
                if (r.depth > depth) {
                    break;
                }
                current = r.getNewestRepresentation();
            }
            return current;
        }

        /**
         * Adds a new representation at the given depth
         * @param newRepresentation The representation to add
         * @param depth The depth
         */
        public void addNewRepresentation(R newRepresentation, int depth) {
            assert representationsForLevels.isEmpty() || representationsForLevels.peek().depth <= depth;
            PartnerClassObjectRepresentationForLevel<R> ar = representationsForLevels.peek();
            if (ar == null || ar.depth < depth) {
                representationsForLevels.push(produceRepresentationForLevel(newRepresentation, depth));
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

        PartnerClassObjectRepresentationForLevel<R> produceRepresentationForLevel(R newRepresentation, int level) {
            return new PartnerClassObjectRepresentationForLevel<>(newRepresentation, level);
        }

        @Override
        public String toString() {
            return String.format("SolverRep[%s]{reps=%s}", id, representationsForLevels);
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

        @Override
        public String toString() {
            return String.format("RepForLevel[%s]{reps=%s}", depth, representationsOfLevel);
        }
    }
}
