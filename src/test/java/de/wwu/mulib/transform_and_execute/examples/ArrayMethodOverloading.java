package de.wwu.mulib.transform_and_execute.examples;

public class ArrayMethodOverloading {

    public static int check(int[][] is) {
        return is[0][0];
    }

    public static int check(double[][] ds) {
        return (int) ds[0][1];
    }

    public static GraphEdge check(GraphEdge[] graphEdges) {
        return graphEdges[0];
    }

    public static GraphEdge check(GraphEdge[][] graphEdges) {
        return graphEdges[1][1];
    }

    public static GraphEdge[] check1(GraphEdge[] gs) {
        return gs;
    }

    public static GraphEdge[][][] check1(GraphEdge[][] gs) {
        return new GraphEdge[][][] { gs };
    }

    public static GraphEdge[][] check1(GraphEdge[][][] gs) {
        return gs[2];
    }

}
