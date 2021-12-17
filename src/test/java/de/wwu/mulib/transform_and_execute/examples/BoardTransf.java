package de.wwu.mulib.transform_and_execute.examples;

public class BoardTransf {
    final int dim;

    public BoardTransf(int dim) {
        this.dim = dim;
    }

    public boolean isOnBoard(QueenTransf q) {
        if (q.x < 0) return false;
        if (q.x > dim-1) return false;
        if (q.y < 0) return false;
        if (q.y > dim-1) return false;
        return true;
    }

    public boolean threatens(QueenTransf p, QueenTransf q) {
        if (p.x == q.x) return true;
        if (p.y == q.y) return true;
        if (p.x - p.y == q.x - q.y) return true;
        if (p.x + p.y == q.x + q.y) return true;
        return false;
    }
}
