package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.And;
import de.wwu.mulib.constraints.ArrayConstraint;
import de.wwu.mulib.constraints.Constraint;

import java.util.*;

public class IncrementalSolverState<AR> {

    // Each constraint represents one "scope" of a constraint here. That means tha a pop in a managed constraint solver
    // corresponds to a pop here.
    private final ArrayDeque<Constraint> constraints = new ArrayDeque<>();
    private int level = 0;

    // To account for mutability, free arrays are stored via a stack (arraydeque) here
    private final Map<Long, ArrayDeque<AR>> arrayIdToMostRecentRepresentation = new HashMap<>();
    // We need to know at which level an array has gained a new representation, so that we know when to add/remove
    // a array from the Map<Long, ArrayDeque<AR>> above
    private final Map<Integer, List<Long>> levelToArrayWithNewRepresentation = new HashMap<>();
    // Stores all array constraints so that they can be replayed if needed // TODO more efficient way?
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
        popArrayConstraintForLevel(level);
        constraints.poll();
        level--;
    }

    public AR getCurrentArrayRepresentation(long id) {
        ArrayDeque<AR> arReps = arrayIdToMostRecentRepresentation.get(id);
        if (arReps == null) {
            return null;
        } else {
            return arReps.peek();
        }
    }

    public void addArrayConstraintAtLevel(ArrayConstraint ac) {
        while (arrayConstraints.size() <= level) {
            arrayConstraints.add(new ArrayList<>());
        }
        arrayConstraints.get(level).add(ac);
    }

    // Adds an ArrayConstraint to regard mutable arrays as a special case.
    public void addRepresentationInitializingArrayConstraint(ArrayConstraint constraint, AR newRepresentation) {
        // Add the array constraint at the respective depth
        List<Long> c = levelToArrayWithNewRepresentation.computeIfAbsent(level, k -> new ArrayList<>());
        c.add(constraint.getArrayId());
        // Initialize/add new array representation
        ArrayDeque<AR> arArrayDeque = arrayIdToMostRecentRepresentation.get(constraint.getArrayId());
        assert arArrayDeque == null || arArrayDeque.isEmpty() || constraint.getType() == ArrayConstraint.Type.STORE;
        if (arArrayDeque == null) {
            arArrayDeque = new ArrayDeque<>();
            arrayIdToMostRecentRepresentation.put(constraint.getArrayId(), arArrayDeque);
        }
        arArrayDeque.push((AR) newRepresentation);
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

    public static IncrementalSolverState newInstance(MulibConfig config) {
        return new IncrementalSolverState(config);
    }

    private void popArrayConstraintForLevel(int level) {
        assert levelToArrayWithNewRepresentation.keySet().stream().noneMatch(k -> k > level);
        if (arrayConstraints.size() > level) {
            arrayConstraints.get(level).clear();
        }
        // Check if popped level contains array constraints
        List<Long> arrayConstraintsOfDepth = levelToArrayWithNewRepresentation.get(level);
        levelToArrayWithNewRepresentation.remove(level);
        if (arrayConstraintsOfDepth != null) {
            // Check if we have to adapt the most recent representation of the free array. This is the case,
            // if there has been an array constraint
            for (Long acId : arrayConstraintsOfDepth) {
                arrayIdToMostRecentRepresentation.get(acId).pop();
            }
        }
    }
}
