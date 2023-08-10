package de.wwu.mulib.examples;

import de.wwu.mulib.Mulib;

import static de.wwu.mulib.Mulib.assume;

public class TravelingSalespersonProblem {

    public static class DirectedEdge {
        public final int n0, n1;
        public boolean visited;
        DirectedEdge(int n0, int n1) {
            this.n0 = n0;
            this.n1 = n1;
        }
    }

    public static class Route {
        public final DirectedEdge[] path;
        private int currentEdgeNumber;

        Route(int numberEdges) {
            this.path = new DirectedEdge[numberEdges];
            this.currentEdgeNumber = 0;
        }

        void addToPath(DirectedEdge directedEdge) {
            path[currentEdgeNumber] = directedEdge;
            directedEdge.visited = true;
            currentEdgeNumber++;
        }

        boolean edgeAdjacentToLastEdge(DirectedEdge e) {
            if (currentEdgeNumber == 0) {
                return true;
            } else {
                DirectedEdge lastDirectedEdge = path[currentEdgeNumber - 1];
                return e.n0 == lastDirectedEdge.n1;
            }
        }

        boolean firstAndLastNodeAreSame() {
            return path[0].n0 == path[path.length - 1].n1;
        }

        boolean edgeMissing() {
            return currentEdgeNumber < path.length;
        }

        public DirectedEdge[] getPath() {
            return path;
        }
    }

    public static Route getRoute(DirectedEdge[] directedEdges) {
        Route result = new Route(directedEdges.length);
        do {
            int i = Mulib.freeInt();
            DirectedEdge chosenDirectedEdge = directedEdges[i];
            assume(!chosenDirectedEdge.visited);
            assume(result.edgeAdjacentToLastEdge(chosenDirectedEdge));
            result.addToPath(chosenDirectedEdge);
        } while (result.edgeMissing());
        assume(result.firstAndLastNodeAreSame());
        return result;
    }

}
