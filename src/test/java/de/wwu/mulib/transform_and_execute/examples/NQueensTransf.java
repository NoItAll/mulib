package de.wwu.mulib.transform_and_execute.examples;

import de.wwu.mulib.Mulib;

public class NQueensTransf {

    public final static int n = 8;

    public static QueenTransf[] solve() {
        BoardTransf board = new BoardTransf(n);
        QueenTransf[] qs = new QueenTransf[n];
        for (int i = 0; i < n; i++) {
            QueenTransf q = new QueenTransf();
            q.x = Mulib.rememberedFreeInt("q.x" + i);
            q.y = Mulib.rememberedFreeInt("q.y" + i);
            qs[i] = q;
        }
        for (int i = 0; i < n; i++) {
            boolean valid = board.isOnBoard(qs[i]);
            if (!valid) {
                throw Mulib.fail();
            }

            for (int j = i+1; j < n; j++) {
                boolean t = board.threatens(qs[i], qs[j]);
                if (t) {
                    throw Mulib.fail();
                }
            }
        }
        return qs;
    }

    public static QueenTransf[] solveAlt() {
        BoardTransf board = new BoardTransf(n);
        QueenTransf[] qs = new QueenTransf[n];
        for (int i = 0; i < n; i++) {
            QueenTransf q = new QueenTransf();
            q.x = i;
            q.y = Mulib.rememberedFreeInt("y" + i);
            qs[i] = q;
        }
        for (int i = 0; i < n; i++) {
            boolean valid = board.isOnBoard(qs[i]);
            if (!valid) {
                throw Mulib.fail();
            }

            for (int j = i+1; j < n; j++) {
                boolean t = board.threatens(qs[i], qs[j]);
                if (t) {
                    throw Mulib.fail();
                }
            }
        }
        return qs;
    }

}