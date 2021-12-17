package de.wwu.mulib.transform_and_execute.examples.mit_examples;
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

import de.wwu.mulib.Mulib;
// HW: Adapted to fit Mulib, taken from https://github.com/sosy-lab/sv-benchmarks/blob/99d37c5b4072891803b9e5c154127c912477f705/java/jayhorn-recursive/InfiniteLoop/Main.java
public class InfiniteLoop {
    public static void exec0() { // HW: Changed from main to exec0
        int i = 0;
        boolean b = Mulib.freeBoolean();

        while (true) {
            i++;
            assert (b);
        }
    }

    public static void exec1() { // HW: Added as infinite loop which cannot be ended by a symbolic budget
        int i = 0;
        boolean b = Mulib.freeBoolean();

        while (true) {
            i++;
        }
    }
}