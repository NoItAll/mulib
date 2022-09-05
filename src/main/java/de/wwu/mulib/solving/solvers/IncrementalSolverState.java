package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.*;

public class IncrementalSolverState<AR> {

    // Each constraint represents one "scope" of a constraint here. That means tha a pop in a managed constraint solver
    // corresponds to a pop here.
    private final ArrayDeque<Constraint> constraints = new ArrayDeque<>();
    private int level = 0;

    // To account for mutability, free arrays are stored via a stack (arraydeque) here
    // We need to know at which level an array has gained a new representation, so that we know when to add/remove
    // a array from the Map<Sint, ArrayDeque<AR>> above
    // We also want to preserve the order in which the constraints are added!
    private final List<List<ArrayConstraint>> arrayConstraints = new ArrayList<>();

    private final SymbolicArrayStates<AR> symbolicArrayStates;
    private final boolean concolic;
    private IncrementalSolverState(MulibConfig config) {
        this.concolic = config.CONCOLIC;
        this.symbolicArrayStates = new SymbolicArrayStates<>(config);
    }

    public static class SymbolicArrayStates<AR> {
        public final boolean enableInitializeFreeArraysWithNull;
        public final boolean aliasingForFreeArrays;
        public final boolean useEagerIndexesForFreeArrayObjectElements;
        private final Map<Sint, ArrayRepresentation<AR>> arrayIdToMostRecentRepresentation = new HashMap<>();

        private SymbolicArrayStates(MulibConfig config) {
            this.enableInitializeFreeArraysWithNull = config.ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL;
            this.aliasingForFreeArrays = config.ALIASING_FOR_FREE_ARRAYS;
            this.useEagerIndexesForFreeArrayObjectElements = config.USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS;
        }

        public ArrayRepresentation<AR> getArraySolverRepresentationForId(Sint arrayId) {
            return arrayIdToMostRecentRepresentation.get(arrayId);
        }
    }

    public SymbolicArrayStates<AR> getSymbolicArrayStates() {
        return symbolicArrayStates;
    }

    protected void addConstraint(Constraint c) {
        // We conjoin the previous with the current constraint so that the uppermost constraint is still a valid
        // representation of the current constraint scope
        Constraint previousTop = constraints.pollFirst();
        constraints.push(And.newInstance(previousTop, c));
    }

    protected void pushConstraint(Constraint c) {
        constraints.push(c);
        level++;
    }

    protected void popConstraint() {
        // Check whether we need to update represented arrays
        popArrayConstraintForLevel();
        constraints.poll();
        level--;
    }

    public AR getCurrentArrayRepresentation(Sint arrayId) {
        ArrayRepresentation<AR> ar = _getArrayRepresentation(arrayId);
        return ar == null ? null : ar.getNewestRepresentation();
    }

    public void addArrayConstraint(ArrayAccessConstraint ac) {
        assert _getArrayRepresentation(ac.getArrayId()) != null;
        while (arrayConstraints.size() <= level) {
            arrayConstraints.add(new ArrayList<>());
        }
        arrayConstraints.get(level).add(ac);
    }

    public void initializeArrayRepresentation(ArrayInitializationConstraint constraint, AR initialRepresentation) {
        assert _getArrayRepresentation(constraint.getArrayId()) == null || _getArrayRepresentation(constraint.getArrayId()).getNewestRepresentation() == null : "Array was already initialized!";
        ArrayRepresentation<AR> ar = new ArrayRepresentation<>(constraint.getArrayId());
        ar.addNewRepresentation(initialRepresentation, level);
        symbolicArrayStates.arrayIdToMostRecentRepresentation.put(constraint.getArrayId(), ar);
    }

    public void addNewRepresentationInitializingArrayConstraint(ArrayAccessConstraint constraint, AR newRepresentation) {
        // Initialize/add new array representation
        ArrayRepresentation<AR> ar = _getArrayRepresentation(constraint.getArrayId());
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

    public int getLevel() {
        return level;
    }

    @SuppressWarnings("rawtypes")
    public static IncrementalSolverState newInstance(MulibConfig config) {
        return new IncrementalSolverState(config);
    }

    private void popArrayConstraintForLevel() {
        // Check if popped level contains array constraints
        for (ArrayRepresentation<AR> ar : symbolicArrayStates.arrayIdToMostRecentRepresentation.values()) {
            ar.popRepresentationsOfLevel(level);
        }
        if (arrayConstraints.size() > level) {
            arrayConstraints.get(level).clear();
        }
    }

    private ArrayRepresentation<AR> _getArrayRepresentation(Sint arrayId) {
        return symbolicArrayStates.arrayIdToMostRecentRepresentation.get(arrayId);
    }

    public static class ArrayRepresentation<AR> {
        // Array that is represented
        final Sint arrayId;
        // Information for each level, including array constraints and the representation per level
        final ArrayDeque<ArrayRepresentationForLevel<AR>> arrayRepresentationsForLevels;
        ArrayRepresentation(Sint arrayId) {
            this.arrayId = arrayId;
            this.arrayRepresentationsForLevels = new ArrayDeque<>();
        }
        public AR getNewestRepresentation() {
            ArrayRepresentationForLevel<AR> resultWrapper = arrayRepresentationsForLevels.peek();
            if (resultWrapper == null) {
                return null;
            }
            return resultWrapper.getNewestRepresentation();
        }

        public void addNewRepresentation(AR newRepresentation, int level) {
            assert arrayRepresentationsForLevels.isEmpty() || arrayRepresentationsForLevels.peek().depth <= level;
            ArrayRepresentationForLevel<AR> ar = arrayRepresentationsForLevels.peek();
            if (ar == null || ar.depth < level) {
                arrayRepresentationsForLevels.push(new ArrayRepresentationForLevel<>(newRepresentation, level));
            } else {
                ar.addRepresentation(newRepresentation);
            }
        }

        void popRepresentationsOfLevel(int level) {
            ArrayRepresentationForLevel<AR> arfl = arrayRepresentationsForLevels.peek();
            assert arfl == null || arfl.depth <= level;
            if (arfl != null && arfl.depth == level) {
                arrayRepresentationsForLevels.pop();
            }
            assert arrayRepresentationsForLevels.isEmpty() || arrayRepresentationsForLevels.peek().depth < level;
        }
    }

    private static class ArrayRepresentationForLevel<AR> {
        final int depth;
        final ArrayDeque<AR> arrayRepresentationsOfLevel;
        ArrayRepresentationForLevel(AR arrayRepresentation, int depth) {
            this.arrayRepresentationsOfLevel = new ArrayDeque<>();
            this.arrayRepresentationsOfLevel.add(arrayRepresentation);
            this.depth = depth;
        }

        void addRepresentation(AR newArrayRepresentation) {
            this.arrayRepresentationsOfLevel.push(newArrayRepresentation);
        }

        AR getNewestRepresentation() {
            return this.arrayRepresentationsOfLevel.peek();
        }
    }

}
