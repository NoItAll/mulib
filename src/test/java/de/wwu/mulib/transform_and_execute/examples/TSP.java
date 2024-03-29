package de.wwu.mulib.transform_and_execute.examples;

import de.wwu.mulib.Mulib;

import static de.wwu.mulib.Mulib.*;

/**
 * Copyright (c) 2011, Regents of the University of California All rights reserved.
 *
 * <p>Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * <p>1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * <p>2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * <p>3. Neither the name of the University of California, Berkeley nor the names of its
 * contributors may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * <p>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/* 2021-11-14 Taken and adjusted from https://github.com/sosy-lab/sv-benchmarks/blob/master/java/algorithms/Tsp-FunSat01/Main.java
    : LT */

/* 2023-07-26 Taken as an example to integrate Mulib with Dacite (https://github.com/dacite-defuse/DynamicDefUse/); - modified by adding getters : HW */


public class TSP {
    private int N;
    private int D[][];
    private boolean visited[];
    private int best;

    public int nCalls;

    public TSP() {}

    public TSP(int N, int D[][]) {
        this.N = N;
        this.D = D;
        this.visited = new boolean[N];
        this.nCalls = 0;
    }

    public int solve() {
        best = 2000000; // Adapted since otherwise an integer overflow occurs.

        for (int i = 0; i < N; i++) {
            visited[i] = false;
        }

        visited[0] = true;
        search(0, 0, N - 1);

        return best;
    }

    private int bound(int src, int length, int nLeft) {
        return length;
    }

    private void search(int src, int length, int nLeft) {
        nCalls++;

        if (nLeft == 0) {
            if (length + D[src][0] < best) {
                best = length + D[src][0];
            }
            return;
        }

        if (bound(src, length, nLeft) >= best) {
            return;
        }

        for (int i = 0; i < N; i++) {
            if (visited[i]) {
                continue;
            }

            visited[i] = true;
            search(i, length + D[src][i], nLeft - 1);
            visited[i] = false;
        }
    }

    public static int execute() {
        TSP tsp = Mulib.rememberedFreeObject("arg0", TSP.class);
        int result = tsp.solve();
        Mulib.remember(tsp, "arg0AfterExec");
        return result;
    }

    public static int driver() {
        TSP tsp = new TSP(freeInt(), freeObject(int[][].class));
        Mulib.remember(tsp, "arg0");
        assume(tsp.visited.length == tsp.N);
        assume(tsp.N <= 3);
        int result = tsp.solve();
        Mulib.remember(tsp, "arg0AfterExec");
        return result;
    }

    public int getN() {
        return N;
    }

    public int[][] getD() {
        return D;
    }

    public boolean[] getVisited() {
        return visited;
    }

    public int getBest() {
        return best;
    }

    public int getnCalls() {
        return nCalls;
    }
}