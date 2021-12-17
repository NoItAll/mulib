package de.wwu.mulib.transform_and_execute.examples.mit_examples;

import de.wwu.mulib.Mulib;

/*
 * Origin of the benchmark:
 *     license: MIT (see /java/jayhorn-recursive/LICENSE)
 *     repo: https://github.com/jayhorn/cav_experiments.git
 *     branch: master
 *     root directory: benchmarks/recursive
 * The benchmark was taken from the repo: 24 January 2018
 */
// Copyright <YEAR> <COPYRIGHT HOLDER>
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

// 2021-09-15 HW: Taken from https://github.com/sosy-lab/sv-benchmarks/blob/master/java/jayhorn-recursive/SatHanoi01/Main.java
public class SatHanoi01Transf {
    // 2021-09-15 HW: Adjusted to fit to current state of Mulib. static variables and methods are turned to virtual ones,
    //  additionally, assert has been removed for now.
    int counter;

    /*
     * This function returns the optimal amount of steps,
     * needed to solve the problem for n-disks
     */
    int hanoi(int n) {
        if (n == 1) {
            return 1;
        }
        return 2 * (hanoi(n - 1)) + 1;
    }

    /*
     * This applies the known algorithm, without executing it (so no arrays).
     * But the amount of steps is counted in a global variable.
     */
    void applyHanoi(int n, int from, int to, int via) {
        if (n == 0) {
            return;
        }
        // increment the number of steps
        counter++;
        applyHanoi(n - 1, from, via, to);
        applyHanoi(n - 1, via, to, from);
    }

    // 2021-09-23 HW: Changed to parameterless function.
    public static int exec() {
        int n = Mulib.namedFreeInt("n");
        SatHanoi01Transf s = new SatHanoi01Transf();

        if (n < 1 || n > 10) {
            throw Mulib.fail();
        }
        s.counter = 0;
        s.applyHanoi(n, 1, 3, 2);
        int result = s.hanoi(n);
        // result and the counter should be the same!
        if (result == s.counter) {
            return result;
        } else {
            throw new IllegalStateException();
        }
    }
}