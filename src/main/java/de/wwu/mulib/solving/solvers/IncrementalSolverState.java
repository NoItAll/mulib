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
    private final List<List<PartnerClassObjectConstraint>> partnerClassObjectConstraints = new ArrayList<>();

    private final SymbolicArrayStates<AR> symbolicArrayStates;
    private IncrementalSolverState(MulibConfig config) {
        this.symbolicArrayStates = new SymbolicArrayStates<>(config);
    }

    public static abstract class SymbolicIdentityHavingSubstitutedVarStates<R extends IdentityHavingSubstitutedVarRepresentation> {
        final Map<Sint, R> idToMostRecentRepresentation = new HashMap<>();
        SymbolicIdentityHavingSubstitutedVarStates(MulibConfig mc) {
        }

        public R getRepresentationForId(Sint id) {
            return idToMostRecentRepresentation.get(id);
        }
    }

    public static class SymbolicArrayStates<S> extends SymbolicIdentityHavingSubstitutedVarStates<ArrayRepresentation<S>> {
        SymbolicArrayStates(MulibConfig config) {
            super(config);
        }
    }

    public static class SymbolicPartnerClassObjectStates<S> extends SymbolicIdentityHavingSubstitutedVarStates<PartnerClassObjectRepresentation<S>> {
        SymbolicPartnerClassObjectStates(MulibConfig config) {
            super(config);
        }
    }

    public SymbolicArrayStates<AR> getSymbolicArrayStates() {
        return symbolicArrayStates;
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
        symbolicArrayStates.idToMostRecentRepresentation.put(constraint.getArrayId(), ar);
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
        for (ArrayRepresentation<AR> ar : symbolicArrayStates.idToMostRecentRepresentation.values()) {
            ar.popRepresentationsOfLevel(level);
        }
        if (arrayConstraints.size() > level) {
            arrayConstraints.get(level).clear();
        }
    }

    private ArrayRepresentation<AR> _getArrayRepresentation(Sint arrayId) {
        return symbolicArrayStates.idToMostRecentRepresentation.get(arrayId);
    }


    public static class IdentityHavingSubstitutedVarRepresentation<R> {
        // Array that is represented
        final Sint id;
        // Information for each level, including array constraints and the representation per level
        final ArrayDeque<IdentityHavingSubstitutedVarRepresentationForLevel<R>> representationsForLevels;
        IdentityHavingSubstitutedVarRepresentation(Sint id) {
            this.id = id;
            this.representationsForLevels = new ArrayDeque<>();
        }
        public R getNewestRepresentation() {
            IdentityHavingSubstitutedVarRepresentationForLevel<R> resultWrapper = representationsForLevels.peek();
            if (resultWrapper == null) {
                return null;
            }
            return resultWrapper.getNewestRepresentation();
        }

        public void addNewRepresentation(R newRepresentation, int level) {
            assert representationsForLevels.isEmpty() || representationsForLevels.peek().depth <= level;
            IdentityHavingSubstitutedVarRepresentationForLevel<R> ar = representationsForLevels.peek();
            if (ar == null || ar.depth < level) {
                representationsForLevels.push(new ArrayRepresentationForLevel<>(newRepresentation, level));
            } else {
                ar.addRepresentation(newRepresentation);
            }
        }

        void popRepresentationsOfLevel(int level) {
            IdentityHavingSubstitutedVarRepresentationForLevel<R> arfl = representationsForLevels.peek();
            assert arfl == null || arfl.depth <= level;
            if (arfl != null && arfl.depth == level) {
                representationsForLevels.pop();
            }
            assert representationsForLevels.isEmpty() || representationsForLevels.peek().depth < level;
        }
    }

    public static class PartnerClassObjectRepresentation<PR> extends IdentityHavingSubstitutedVarRepresentation<PR> {
        PartnerClassObjectRepresentation(Sint id) {
            super(id);
        }
    }

    public static class ArrayRepresentation<AR> extends IdentityHavingSubstitutedVarRepresentation<AR> {
        ArrayRepresentation(Sint arrayId) {
            super(arrayId);
        }
    }


    private static class IdentityHavingSubstitutedVarRepresentationForLevel<R> {
        final int depth;
        final ArrayDeque<R> representationsOfLevel;
        IdentityHavingSubstitutedVarRepresentationForLevel(R representation, int depth) {
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

    private static class PartnerClassObjectRepresentationForLevel<PR> extends IdentityHavingSubstitutedVarRepresentationForLevel<PR> {
        PartnerClassObjectRepresentationForLevel(PR representation, int depth) {
            super(representation, depth);
        }
    }

    private static class ArrayRepresentationForLevel<AR> extends IdentityHavingSubstitutedVarRepresentationForLevel<AR> {
        ArrayRepresentationForLevel(AR representation, int depth) {
            super(representation, depth);
        }
    }

}
