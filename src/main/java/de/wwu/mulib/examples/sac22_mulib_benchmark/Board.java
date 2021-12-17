package de.wwu.mulib.examples.sac22_mulib_benchmark;

public class Board {
    final int dim;

    public Board(int dim) {
        this.dim = dim;
    }

    public boolean isOnBoard(Queen q) {
        if (q.x < 0) return false;
        if (q.x > dim-1) return false;
        if (q.y < 0) return false;
        if (q.y > dim-1) return false;
        return true;
    }

    public boolean threatens(Queen p, Queen q) {
        if (p.x == q.x) return true;
        if (p.y == q.y) return true;
        if (p.x - p.y == q.x - q.y) return true;
        if (p.x + p.y == q.x + q.y) return true;
        return false;
    }
}
