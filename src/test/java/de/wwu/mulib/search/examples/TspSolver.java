package de.wwu.mulib.search.examples;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


// HW: The following example has been heavily adapted. All primitive values have been changed to the (potentially) symbolic
// types of the Mulib library
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
public class TspSolver {
    private Sint N;
    private Sint[][] D;
    private Sbool[] visited;
    private Sint best;

    public Sint nCalls;

    public static Sint exec() {
        SymbolicExecution se = SymbolicExecution.get();
        final int N = 4; // Adapted so that we can fully explore the search space without using a budget; Muli does not support bounded search
        Sint D[][] = new Sint[N][N];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                Sint next = se.namedSymSint(i + "," + j);
                if(next.ltChoice(se.concSint(1), se)) throw Mulib.fail();
                D[i][j] = next;
            }
        }

        TspSolver tspSolver = new TspSolver();

        tspSolver.N = se.concSint(N);
        tspSolver.D = D;
        tspSolver.visited = new Sbool[N];
        tspSolver.nCalls = se.concSint(0);

        Sint sln = tspSolver.solve();
        return sln;
    }

    public TspSolver() {}

    public Sint solve() {
        SymbolicExecution se = SymbolicExecution.get();
        best = se.concSint(2000000); // Adapted since otherwise an integer overflow occurs in Muli.

        for (int i = 0; se.concSint(i).ltChoice(N, se); i++) visited[i] = se.concSbool(false);

        visited[0] = se.concSbool(true);
        search(se.concSint(0), se.concSint(0), N.sub(se.concSint(1), se));

        return best;
    }

    private Sint bound(Sint src, Sint length, Sint nLeft) {
        return length;
    }

    private void search(Sint src, Sint length, Sint nLeft) {
        SymbolicExecution se = SymbolicExecution.get();
        nCalls.add(se.concSint(1), se);

        if (nLeft.eqChoice(se)) {
            if (length.add(D[(Integer) se.concretize(src)][0], se).ltChoice(best, se)) best = length.add(D[(Integer) se.concretize(src)][0], se);
            return;
        }

        if (bound(src, length, nLeft).gteChoice(best, se)) return;

        for (int i = 0; se.concSint(i).ltChoice(N, se); i++) {
            if (visited[i].boolChoice(se)) continue;

            visited[i] = se.concSbool(true);
            search(se.concSint(i), length.add(D[(Integer) se.concretize(src)][i], se), nLeft.sub(se.concSint(1), se));
            visited[i] = se.concSbool(false);
        }
    }

    @Test
    public void testExec() {
        TestUtility.getAllSolutions(this::_checkExec, "exec");
    }

    private List<PathSolution> _checkExec(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "exec",
                TspSolver.class,
                mb,
                false
        );
        assertFalse(
                result.parallelStream().anyMatch(ps -> {
                    for (PathSolution psInner : result) {
                        if (psInner == ps) continue;
                        if (ps.getSolution().labels.getIdToLabel().equals(
                                psInner.getSolution().labels.getIdToLabel())) {
                            return true;
                        }
                    }
                    return false;
                })
        );
        assertEquals(970, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        return result;
    }
}