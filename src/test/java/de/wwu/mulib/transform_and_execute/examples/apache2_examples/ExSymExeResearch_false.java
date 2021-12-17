package de.wwu.mulib.transform_and_execute.examples.apache2_examples;

/*
 * Origin of the benchmark:
 *     repo: https://babelfish.arc.nasa.gov/hg/jpf/jpf-symbc
 *     branch: updated-spf
 *     root directory: src/tests/gov/nasa/jpf/symbc
 * The benchmark was taken from the repo: 24 January 2018
 */
/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * Symbolic Pathfinder (jpf-symbc) is licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// package gov.nasa.jpf.symbc;

import de.wwu.mulib.Mulib;

// HW: Adapted to fit Mulib, taken from https://github.com/sosy-lab/sv-benchmarks/blob/99d37c5b4072891803b9e5c154127c912477f705/java/jpf-regression/ExSymExeResearch_false/Main.java
public class ExSymExeResearch_false {

    public static void exec() { // HW: changed from main to exec
        int x = Mulib.freeInt();
        if (x < 0) return;
        int y = 5;
        ExSymExeResearch_false inst = new ExSymExeResearch_false();
        assert inst.test(x, y) == 2;
    }

    public int test(int a, int b) { // invokevirtual
        int result = 0;
//        System.out.println("Testing ExSymExeResearch"); HW: Removed
        if (a >= 0 && a < 100 && b >= 0 && b < 100) {
            int sum = a + b;
            int diff = a - b;
            int temp;

            if (sum > 0) temp = a;
            else temp = b;
            if (temp < diff) result = temp;
            else result = diff;
        }
        return result;
    }
}