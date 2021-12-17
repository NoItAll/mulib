package de.wwu.mulib.examples.sac22_mulib_benchmark;

import de.wwu.mulib.Mulib;

public class NQueens {

    public static Queen[] solve() {
        final int n = 14;
        Board board = new Board(n);
        Queen[] qs = new Queen[n];
        for (int i = 0; i < n; i++) {
            Queen q = new Queen();
            q.x = Mulib.trackedFreeInt("q.x" + i);
            q.y = Mulib.trackedFreeInt("q.y" + i);
            qs[i] = q;
        }
        for (int i = 0; i < n; i++) {
            boolean valid = board.isOnBoard(qs[i]);
            if (!valid) {
                throw Mulib.fail();
            }

            for (int j = i + 1; j < n; j++) {
                boolean t = board.threatens(qs[i], qs[j]);
                if (t) {
                    throw Mulib.fail();
                }
            }
        }
        return qs;
    }
}