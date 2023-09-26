package de.wwu.mulib.examples;

import static de.wwu.mulib.Mulib.*;

public class HamiltonianCycleProblem {

    public static class Edge {
        private final Node n0;
        private final Node n1;

        public Edge(Node n0, Node n1) {
            if (n0.equals(n1)) {
                throw new IllegalArgumentException("Edge must not be loop");
            }
            this.n0 = n0;
            this.n1 = n1;
        }

        boolean hasNodeAsStartNode(Node n) {
            // Any node can be the start node
            return n0 == n || n1 == n;
        }

        public Node[] getNodes() {
            return new Node[] { n0, n1 };
        }

        Node getAnyUnvisitedReachableNode() {
            Node n = getAnyReachableNode();
            assume(!n.visited);
            return n;
        }

        Node getAnyReachableNode() {
            return pickFrom(n0, n1);
        }

        Node getAnyStartNode() {
            // Undirected, start does not matter
            return n0;
        }
    }

    public static class Node {
        // Here to validate the routes:
        public int id;
        boolean visited = false;
        public Node(int id) {
            this.id = id;
        }

        public void visit() {
            this.visited = true;
        }

        boolean equals(Node n) {
            return n == this;
        }

        @Override
        public String toString() {
            return String.format("Node[%s]", id);
        }
    }

    public static Node[] solve(int numberNodes, Edge[] edges) {
        assume(edges.length > 0 && numberNodes > 0);
        // For a cycle, for n nodes, our path consists of n+1 nodes
        Node[] result = new Node[numberNodes + 1];
        // Pick arbitrary first edge and a starting node
        Edge currentEdge = edges[0];
        Node currentNodeToReach = currentEdge.getAnyStartNode();
        // Mark the node as visited
        currentNodeToReach.visit();
        // The first node on the path is this node
        result[0] = currentNodeToReach;
        for (int numberAddedNodes = 1; numberAddedNodes < result.length - 1; numberAddedNodes++) {
            // Get a reachable, unvisited node given the current edge
            currentNodeToReach = currentEdge.getAnyUnvisitedReachableNode();
            // Visit the current node
            currentNodeToReach.visit();
            // Add the node to the tour
            result[numberAddedNodes] = currentNodeToReach;
            // Pick a new edge
            currentEdge = pickFrom(edges);
            // The new edge must have the current node as a start node
            assume(currentEdge.hasNodeAsStartNode(currentNodeToReach));
        }
        // First node in tour must be last node
        currentNodeToReach = currentEdge.getAnyReachableNode();
        result[result.length - 1] = currentNodeToReach;
        assume(result[0].equals(currentNodeToReach));
        return result;
    }

}
