package de.wwu.mulib.search.choice_points;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.search.trees.Choice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CoverageCfg {
    private final ThreadLocal<CfgNode> currentCfgNode = new ThreadLocal<>();
    private final ThreadLocal<ArrayDeque<CfgNodeDecision>> trailOfDecisions = new ThreadLocal<>();
    private final Map<Long, CfgNode> idToNode = new ConcurrentHashMap<>();
    private final Set<CfgNode> nodesWithUncoveredEdges = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final long totalNumberChoicePointsInSearchRegion;
    private final MulibConfig config;

    public CoverageCfg(MulibConfig config, long totalNumberChoicePointsInSearchRegion) {
        this.config = config;
        this.totalNumberChoicePointsInSearchRegion = totalNumberChoicePointsInSearchRegion;
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

    public enum CoverageInformation {
        BOTH_NOT_COVERED,
        TRUE_BRANCH_NOT_COVERED,
        FALSE_BRANCH_NOT_COVERED,
        ALL_COVERED,
        NO_INFORMATION
    }

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

    public CoverageInformation getCoverageInformationForCurrentNode() { // 2
        return getCoverageInformation(currentCfgNode.get());
    }

    private static CoverageInformation getCoverageInformation(CfgNode node) {
        if (node == null) {
            return CoverageInformation.NO_INFORMATION;
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

    public void traverseCurrentNodeWithDecision(long id, boolean decision) { // 3
        CfgNode node = currentCfgNode.get();
        if (node != null) {
            assert node.id == id;
            getTrail().push(new CfgNodeDecision(node, decision));
        }
    }

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

    public void reset() { // 6
        if (!config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
            throw new MulibIllegalStateException("Must not call if not configured");
        }
        this.trailOfDecisions.remove();
    }

    public BitSet getCoverAndReset() { // 6
        BitSet bitSet = new BitSet((int) this.totalNumberChoicePointsInSearchRegion * 2); // true and false
        for (CfgNodeDecision d : trailOfDecisions.get()) {
            bitSet.set((int) (d.cfgNode.id * 2) + (d.decision ? 0 : 1));
        }
        reset();
        return bitSet;
    }

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

    public boolean fullCoverageAchieved() { // Termination procedure
        if (!config.CFG_TERMINATE_EARLY_ON_FULL_COVERAGE) {
            throw new MulibIllegalStateException("Must not call if not configured");
        }
        return this.nodesWithUncoveredEdges.isEmpty() && this.idToNode.size() == totalNumberChoicePointsInSearchRegion;
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
