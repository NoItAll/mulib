package de.wwu.mulib.search.choice_points;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.search.trees.Choice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple, thread-safe control flow graph meant to compute the coverage with
 * It will be constructed ad-hoc, i.e., initially we do not yet know which nodes are connected with which other nodes
 */
public class CoverageCfg {
    private final ThreadLocal<CfgNode> currentCfgNode = new ThreadLocal<>();
    private final ThreadLocal<ArrayDeque<CfgNodeDecision>> trailOfDecisions = new ThreadLocal<>();
    private final Map<Long, CfgNode> idToNode = new ConcurrentHashMap<>();
    private final Set<CfgNode> nodesWithUncoveredEdges = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final long totalNumberDecisionNodes;
    private final MulibConfig config;

    /**
     * @param config The configuration
     * @param totalNumberDecisionNodes The total number of nodes with choice points in the control flow graph
     *                                 of the search region
     */
    public CoverageCfg(MulibConfig config, long totalNumberDecisionNodes) {
        this.config = config;
        this.totalNumberDecisionNodes = totalNumberDecisionNodes;
    }

    private static class CfgNodeDecision {
        final CfgNode cfgNode;
        final boolean decision;

        CfgNodeDecision(CfgNode cfgNode, boolean decision) {
            this.cfgNode = cfgNode;
            this.decision = decision;
        }
    }

    private static class CfgNode {
        long id;
        boolean trueBranchCovered, falseBranchCovered;
        List<Choice> choices;

        CfgNode(long id) {
            this.id = id;
            this.choices = new ArrayList<>();
        }

        void traverseDecision(long id, boolean trueBranchTraversed) {
            if (this.id != id) {
                throw new MulibIllegalStateException(String.format("ID %s expected but %s found", this.id, id));
            }
            if (trueBranchTraversed) {
                trueBranchCovered = true;
            } else {
                falseBranchCovered = true;
            }
        }

        @Override
        public String toString() {
            return String.format("CfgNode[%s]{trueBranchCovered=%s, falseBranchCovered=%s}",
                    id,
                    trueBranchCovered,
                    falseBranchCovered
            );
        }
    }

    /**
     * Coverage information on the current choice
     */
    public enum CoverageInformation {
        /**
         * Both branches are not covered
         */
        BOTH_NOT_COVERED,
        /**
         * The true-branch is not covered
         */
        TRUE_BRANCH_NOT_COVERED,
        /**
         * The false-branch is not covered
         */
        FALSE_BRANCH_NOT_COVERED,
        /**
         * All branches are covered
         */
        ALL_COVERED,
        /**
         * Occurs for internal choice options (without an identifier) and for manually created search regions
         * where we do not use choice options with a id
         */
        NO_INFO
    }

    /**
     * Checks if this node is already contained in the graph. If this is the case, do nothing.
     * Otherwise, we initialize a new node
     * @param id The identifier of the node
     */
    public void setCurrentCfgNodeIfNecessary(long id) { // 1
        CfgNode node = idToNode.get(id);
        if (node == null) {
            synchronized (this) {
                node = idToNode.get(id);
                if (node == null) { // Re-check
                    node = new CfgNode(id);
                    node.id = id;
                    putIdToNode(node);
                    if (config.CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE || config.CFG_TERMINATE_EARLY_ON_FULL_COVERAGE) {
                        nodesWithUncoveredEdges.add(node);
                    }
                }
            }
        }
        currentCfgNode.set(node);
    }

    /**
     * @return Information on which decision has already been processed
     */
    public CoverageInformation getCoverageInformationForCurrentNode() { // 2
        if (!config.TRANSF_TRANSFORMATION_REQUIRED) {
            return CoverageInformation.NO_INFO;
        }
        CfgNode node = currentCfgNode.get();
        if (node == null) {
            // Is an internal choice
            return CoverageInformation.NO_INFO;
        } else if (!node.trueBranchCovered && !node.falseBranchCovered) {
            return CoverageInformation.BOTH_NOT_COVERED;
        } else if (!node.trueBranchCovered) {
            return CoverageInformation.TRUE_BRANCH_NOT_COVERED;
        } else if (!node.falseBranchCovered) {
            return CoverageInformation.FALSE_BRANCH_NOT_COVERED;
        } else {
            return CoverageInformation.ALL_COVERED;
        }
    }

    /**
     * Adds the decision to the current tail so that we can later determine which branches
     * have been passed
     * @param id The node
     * @param decision The decision
     */
    public void traverseCurrentNodeWithDecision(long id, boolean decision) { // 3
        CfgNode node = currentCfgNode.get();
        if (node != null) {
            assert node.id == id;
            getTrail().push(new CfgNodeDecision(node, decision));
        }
    }

    /**
     * Only used if {@link MulibConfig#CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE} is turned on
     * Will add the choice to those choices that occur on the node in the cfg
     * @param choice The choice that is mapped to this cfg node
     * @param id The identifier of the CFG node
     */
    public void addChoiceForCfgNode(Choice choice, long id) { // 4
        if (config.CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE) {
            CfgNode node = currentCfgNode.get();
            if (node != null) {
                assert node.id == id;
                synchronized (node) {
                    node.choices.add(choice);
                }
            }
        }
        currentCfgNode.remove();
    }

    /**
     * Manifest the trail. We have found a valid point to leave the search region and thus, we can record the path.
     */
    public void manifestTrail() { // 5
        ArrayDeque<CfgNodeDecision> decisions = getTrail();
        for (CfgNodeDecision d : decisions) {
            CfgNode node = d.cfgNode;
            boolean trueTraversedBefore = node.trueBranchCovered;
            boolean falseTraversedBefore = node.falseBranchCovered;
            node.traverseDecision(node.id, d.decision);
            boolean trueTraversedAfter = node.trueBranchCovered;
            boolean falseTraversedAfter = node.falseBranchCovered;
            if ((config.CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE || config.CFG_TERMINATE_EARLY_ON_FULL_COVERAGE)
                    && ((trueTraversedAfter && falseTraversedAfter) && (trueTraversedBefore != trueTraversedAfter || falseTraversedBefore != falseTraversedAfter))) {
                nodesWithUncoveredEdges.remove(node);
                synchronized (node) {
                    node.choices.clear(); // Not needed any longer
                }
            }
        }
    }

    /**
     * Resets the current trail.
     * This must be done if we backtrack so that we do not record too many edges in the cfg at the end
     */
    public void reset() { // 6
        if (!config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
            throw new MulibIllegalStateException("Must not call if not configured");
        }
        this.trailOfDecisions.remove();
    }

    /**
     * Calculates the cover from the trail.
     * Then deletes the trail.
     * @return The cover
     */
    public BitSet getCoverAndReset() { // 6
        BitSet bitSet = new BitSet((int) this.totalNumberDecisionNodes * 2); // true and false
        for (CfgNodeDecision d : trailOfDecisions.get()) {
            bitSet.set((int) (d.cfgNode.id * 2) + (d.decision ? 0 : 1));
        }
        reset();
        return bitSet;
    }

    /**
     * @param co The choice option that is checked
     * @return true if there are still edges for the node that is mapped to the choice option
     * where one branch has not been covered, else false
     */
    public boolean hasUncoveredEdges(Choice.ChoiceOption co) {
        if (!config.CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE) {
            throw new MulibIllegalStateException("Must only call this method for checking whether a choice option belongs to an unevaluated edge in the CFG if" +
                    " while creating a new execution");
        }
        for (CfgNode n : nodesWithUncoveredEdges) {
            if (n.choices.contains(co.getChoice())
                    && ((!n.trueBranchCovered && co.choiceOptionNumber == 0) || (!n.falseBranchCovered && co.choiceOptionNumber == 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true, if all branches have been covered, else false
     */
    public boolean fullCoverageAchieved() { // Termination procedure
        if (!config.CFG_TERMINATE_EARLY_ON_FULL_COVERAGE) {
            throw new MulibIllegalStateException("Must not call if not configured");
        }
        return this.nodesWithUncoveredEdges.isEmpty() && this.idToNode.size() == totalNumberDecisionNodes;
    }

    private ArrayDeque<CfgNodeDecision> getTrail() {
        ArrayDeque<CfgNodeDecision> result = trailOfDecisions.get();
        if (result == null) {
            result = new ArrayDeque<>();
            trailOfDecisions.set(result);
        }
        return result;
    }

    private void putIdToNode(CfgNode cfgNode) {
        CfgNode previous = idToNode.put(cfgNode.id, cfgNode);
        if (previous != null) {
            assert previous.id == cfgNode.id;
            throw new MulibIllegalStateException("Must not occur");
        }
    }
}
