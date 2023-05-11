package de.wwu.mulib.transform_and_execute.examples;

import de.wwu.mulib.Mulib;

import java.util.ArrayList;

public class GraphColoring {

    int n = 9;    // number of nodes
    int c = 9;    // number of colors
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
            int color = Mulib.rememberedFreeInt("" + i);
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