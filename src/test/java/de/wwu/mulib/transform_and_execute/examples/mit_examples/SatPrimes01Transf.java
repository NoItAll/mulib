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
import de.wwu.mulib.exceptions.MulibRuntimeException;

// HW: Taken and adjusted to fit Mulib; https://github.com/sosy-lab/sv-benchmarks/blob/master/java/jayhorn-recursive/SatPrimes01/Main.java
public class SatPrimes01Transf {

    // Multiplies two integers n and m
    static int mult(int n, int m) {
        if (m < 0) {
            return mult(n, -m);
        }
        if (m == 0) {
            return 0;
        }
        if (m == 1) {
            return 1;
        }
        return n + mult(n, m - 1);
    }

    // Is n a multiple of m?
    static int multiple_of(int n, int m) {
        if (m < 0) {
            return multiple_of(n, -m);
        }
        if (n < 0) {
            return multiple_of(-n, m); // false
        }
        if (m == 0) {
            return 0; // false
        }
        if (n == 0) {
            return 1; // true
        }
        return multiple_of(n - m, m);
    }

    // Is n prime?
    static int is_prime(int n) {
        return is_prime_(n, n - 1);
    }

    static int is_prime_(int n, int m) {
        if (n <= 1) {
            return 0; // false
        } else if (n == 2) {
            return 1; // true
        } else {
            if (m <= 1) {
                return 1; // true
            } else {
                if (multiple_of(n, m) == 0) {
                    return 0; // false
                }
                return is_prime_(n, m - 1);
            }
        }
    }

    // HW: Changed from main
    public static void exec0() {
        int n = Mulib.rememberedFreeInt("n");
        if (n < 1 || n > 3) { // HW: Reduced for test case
            return;
        }
        int result = is_prime(n);
        int f1 = Mulib.rememberedFreeInt("f1");
        if (f1 < 1 || f1 > 3) { // HW: Reduced for test case
            return;
        }
        int f2 = Mulib.rememberedFreeInt("f2");
        if (f1 < 1 || f1 > 3) { // HW: Reduced for test case
            return;
        }

        if (result == 1 && mult(f1, f2) == n && f1 > 1 && f2 > 1) {
            throw new MulibRuntimeException("Must not occur");
        } else {
            return;
        }
    }

    // HW: Added, fixed check of f2, reduced bound so that every solution is calculable,
    // for, e.g. multiple_of(3,2) an endless loop was found
    public static void exec1() {
        int n = Mulib.rememberedFreeInt("n");
        if (n < 1 || n > 2) {
            return;
        }
        int result = is_prime(n);
        int f1 = Mulib.rememberedFreeInt("f1");
        if (f1 < 1 || f1 > 2) {
            return;
        }
        int f2 = Mulib.rememberedFreeInt("f2");
        if (f2 < 1 || f2 > 2) {
            return;
        }

        if (result == 1 && mult(f1, f2) == n && f1 > 1 && f2 > 1) {
            throw new MulibRuntimeException("Must not occur");
        } else {
            return;
        }
    }
}