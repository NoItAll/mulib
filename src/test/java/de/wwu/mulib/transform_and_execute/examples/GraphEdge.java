package de.wwu.mulib.transform_and_execute.examples;

public class GraphEdge {
    int start; int end;
    public GraphEdge(int s, int e) { start = s; end = e; }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}