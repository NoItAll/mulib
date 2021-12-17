package de.wwu.mulib.examples.sac22_mulib_benchmark;

import de.wwu.mulib.Mulib;

import java.util.ArrayList;

// Mirrors GraphColoring in https://github.com/wwu-pi/muli/tree/master/examples/sac22_mulib_benchmark
public class GraphColoring {

    int n = 35;    // number of nodes
    int c = 35;    // number of colors
    int[] colors; // colors of nodes
    ArrayList<GraphEdge> edges = new ArrayList<GraphEdge>();

    public GraphColoring(){
        colors = new int[n];
        generateGraph(); // generate some graph
    }

    public void generateGraph(){
        for(int i=0; i<n; i++)
            for(int j=0; j<n; j++)
                if (i != j)
                    edges.add(new GraphEdge(i,j)); }

    public void generateColoring(){
        for(int i=0; i<n; i++) {
            int color = Mulib.namedFreeInt("" + i);
            if (!(color > 0 && color <= c))
                throw Mulib.fail();
            else colors[i] = color; } }

    public void checkColoring(){
        for(GraphEdge e: edges) {
            if (colors[e.start] == colors[e.end])
                throw Mulib.fail(); } }

    public static int[] exec() {
        GraphColoring gc = new GraphColoring();
        gc.generateColoring();
        gc.checkColoring();
        return gc.colors;
    }
}