package de.wwu.mulib.search.executors;

public enum SearchStrategy {
    BFS, // Breath-first search
    DFS, // Depth-first search
    IDDFS, // Iterative deepening depth-first search
    IDDSAS, // Iterative-deepening deepest shared-search
    DSAS // Deepest shared ancestor-search
}
