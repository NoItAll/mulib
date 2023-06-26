package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.substitutions.PartnerClass;
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
            assert r != null;
            pcor.addNewRepresentation(r, level);
            idToMostRecentRepresentation.put(id, pcor);
        }

        public PartnerClassObjectRepresentation<R> getRepresentationForId(Sint id) {
            return idToMostRecentRepresentation.get(id);
        }
    }

    public void clear() {
        this.constraints.clear();
        this.partnerClassObjectConstraints.clear();
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
        // Check whether we need to update represented partner class objects
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

    public void addArrayConstraint(ArrayConstraint ac) {
        assert _getArrayRepresentation(ac.getPartnerClassObjectId()) != null;
        addIdentityHavingSubstitutedVarConstraint(level, ac);
    }

    public void addPartnerClassObjectConstraint(PartnerClassObjectConstraint pc) {
        assert pc instanceof PartnerClassObjectRememberConstraint || _getPartnerClassObjectRepresentation(pc.getPartnerClassObjectId()) != null;
        addIdentityHavingSubstitutedVarConstraint(level, pc);
    }

    private void addIdentityHavingSubstitutedVarConstraint(int level, PartnerClassObjectConstraint add) {
        while (partnerClassObjectConstraints.size() <= level) {
            partnerClassObjectConstraints.add(new ArrayList<>());
        }
        partnerClassObjectConstraints.get(level).add(add);
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

    public void addNewRepresentationInitializingPartnerClassFieldConstraint(PartnerClassObjectFieldConstraint c, PR newRepresentation) {
        PartnerClassObjectRepresentation<PR> pr = _getPartnerClassObjectRepresentation(c.getPartnerClassObjectId());
        assert pr != null : "Partner class object representation was not initialized via a PartnerClassObjectInitializationConstraint!";
        pr.addNewRepresentation(newRepresentation, level);
    }

    public ArrayDeque<Constraint> getConstraints() {
        return constraints;
    }

    public List<PartnerClassObjectConstraint> getAllPartnerClassObjectConstraintsExcludingRememberConstraints() {
        List<PartnerClassObjectConstraint> result = new ArrayList<>();
        for (List<PartnerClassObjectConstraint> pcocs : partnerClassObjectConstraints) {
            result.addAll(pcocs);
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void popPartnerClassConstraintsForLevel() {
        for (PartnerClassObjectRepresentation<?> pr :
                (Collection<PartnerClassObjectRepresentation>) symbolicPartnerClassObjectStates.idToMostRecentRepresentation.values()) {
            pr.popRepresentationsOfLevel(level);
        }
        // Check if popped level contains partner class object constraints
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

    // Get all PUTFIELD- and GETFIELD-constraints and XALOAD- and XASTORE-constraints before the RememberConstraint
    // has been issued.
    public RememberedPartnerClassObjectContainer[] getContainersForLabelingRememberedValue() {
        List<RememberedPartnerClassObjectContainer> result = new ArrayList<>();
        List<PartnerClassObjectConstraint> constraints = this.getAllPartnerClassObjectConstraintsExcludingRememberConstraints();

        PartnerClassObjectRememberConstraint[] rememberConstraints =
                constraints.stream()
                        .filter(ac -> ac instanceof PartnerClassObjectRememberConstraint)
                        .toArray(PartnerClassObjectRememberConstraint[]::new);
        for (PartnerClassObjectRememberConstraint rememberConstraint : rememberConstraints) {
            List<PartnerClassObjectConstraint> relevantConstraints = new ArrayList<>();
            for (PartnerClassObjectConstraint ac : constraints) {
                if (ac == rememberConstraint) {
                    break;
                } else if (ac instanceof PartnerClassObjectRememberConstraint) {
                    // We do not care about other remember constraints
                    continue;
                }

                // If it is not a remember constraint and the respective remember constraint has not been seen beforehand
                // add the constraint
                relevantConstraints.add(ac);
            }

            result.add(new RememberedPartnerClassObjectContainer(
                    rememberConstraint.getName(),
                    rememberConstraint.getRememberedValue(),
                    relevantConstraints
            ));
        }


        return result.toArray(RememberedPartnerClassObjectContainer[]::new);
    }

    public static class RememberedPartnerClassObjectContainer {
        private final String name;
        private final List<PartnerClassObjectConstraint> partnerClassObjectConstraintsBeforeRemember;
        private final PartnerClass copiedAtState;

        RememberedPartnerClassObjectContainer(
                String name,
                PartnerClass copiedAtState,
                List<PartnerClassObjectConstraint> partnerClassObjectConstraintsBeforeRemember) {
            this.name = name;
            this.partnerClassObjectConstraintsBeforeRemember = partnerClassObjectConstraintsBeforeRemember;
            this.copiedAtState = copiedAtState;
        }

        public String getName() {
            return name;
        }

        public List<PartnerClassObjectConstraint> getPartnerClassObjectConstraintsBeforeRemember() {
            return partnerClassObjectConstraintsBeforeRemember;
        }

        public PartnerClass getCopiedAtRemember() {
            return copiedAtState;
        }
    }
}
