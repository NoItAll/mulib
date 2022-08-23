package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.And;
import de.wwu.mulib.constraints.ArrayConstraint;
import de.wwu.mulib.constraints.Constraint;
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
    private final Map<Sint, ArrayRepresentation> arrayIdToMostRecentRepresentation = new HashMap<>();
    // We also want to preserve the order in which the constraints are added!
    private final List<List<ArrayConstraint>> arrayConstraints = new ArrayList<>();

    private IncrementalSolverState(MulibConfig config) {}

    public void addConstraint(Constraint c) {
        // We conjoin the previous with the current constraint so that the uppermost constraint is still a valid
        // representation of the current constraint scope
        Constraint previousTop = constraints.pollFirst();
        constraints.push(And.newInstance(previousTop, c));
    }

    public void pushConstraint(Constraint c) {
        constraints.push(c);
        level++;
    }

    public void popConstraint() {
        // Check whether we need to update represented arrays
        popArrayConstraintForLevel();
        constraints.poll();
        level--;
    }

    public AR getCurrentArrayRepresentation(Sint arrayId) {
        return _getArrayRepresentation(arrayId).getNewestRepresentation();
    }

    public void addArrayConstraint(ArrayConstraint ac) {
        ArrayRepresentation ar = _getArrayRepresentation(ac.getArrayId());
        assert !ar.isEmpty();
        while (arrayConstraints.size() <= level) {
            arrayConstraints.add(new ArrayList<>());
        }
        arrayConstraints.get(level).add(ac);
    }

    // Adds an ArrayConstraint to regard mutable arrays as a special case.
    public void addRepresentationInitializingArrayConstraint(ArrayConstraint constraint, AR newRepresentation) {
        // Initialize/add new array representation
        ArrayRepresentation ar = _getArrayRepresentation(constraint.getArrayId());
        ar.addNewRepresentation(newRepresentation);
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
        for (ArrayRepresentation ar : arrayIdToMostRecentRepresentation.values()) {
            ar.popRepresentationsOfLevel();
        }
        if (arrayConstraints.size() > level) {
            arrayConstraints.get(level).clear();
        }
    }

    private ArrayRepresentation _getArrayRepresentation(Sint arrayId) {
        ArrayRepresentation ar = arrayIdToMostRecentRepresentation.computeIfAbsent(arrayId, ArrayRepresentation::new);
        return ar;
    }

    private class ArrayRepresentation {
        // Array that is represented
        final Sint arrayId;
        // Information for each level, including array constraints and the representation per level
        final ArrayDeque<ArrayRepresentationForLevel> arrayRepresentationsForLevels;
        ArrayRepresentation(Sint arrayId) {
            this.arrayId = arrayId;
            this.arrayRepresentationsForLevels = new ArrayDeque<>();
        }

        boolean isEmpty() {
            return arrayRepresentationsForLevels.isEmpty();
        }

        AR getNewestRepresentation() {
            ArrayRepresentationForLevel resultWrapper = arrayRepresentationsForLevels.peek();
            if (resultWrapper == null) {
                return null;
            }
            return resultWrapper.getNewestRepresentation();
        }

        void addNewRepresentation(AR newRepresentation) {
            assert arrayRepresentationsForLevels.isEmpty() || arrayRepresentationsForLevels.peek().depth <= level;
            ArrayRepresentationForLevel ar = arrayRepresentationsForLevels.peek();
            if (ar == null || ar.depth < level) {
                arrayRepresentationsForLevels.push(new ArrayRepresentationForLevel(newRepresentation, level));
            } else {
                ar.addRepresentation(newRepresentation);
            }
        }

        void popRepresentationsOfLevel() {
            ArrayRepresentationForLevel arfl = arrayRepresentationsForLevels.peek();
            assert arfl == null || arfl.depth <= level;
            if (arfl != null && arfl.depth == level) {
                arrayRepresentationsForLevels.pop();
            }
            assert arrayRepresentationsForLevels.isEmpty() || arrayRepresentationsForLevels.peek().depth < level;
        }
    }

    private class ArrayRepresentationForLevel {
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
