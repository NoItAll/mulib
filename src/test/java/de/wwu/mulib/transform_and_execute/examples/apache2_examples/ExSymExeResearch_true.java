package de.wwu.mulib.transform_and_execute.examples.apache2_examples;/*
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
import de.wwu.mulib.throwables.MulibRuntimeException;

// HW: Adapted to fit Mulib, taken from https://github.com/sosy-lab/sv-benchmarks/blob/99d37c5b4072891803b9e5c154127c912477f705/java/jpf-regression/ExSymExeResearch_true/Main.java
public class ExSymExeResearch_true {

    public static void exec() {
        int arg = Mulib.freeInt();
        int x = arg > 0 ? arg : -arg;
        int y = 5;
        ExSymExeResearch_true inst = new ExSymExeResearch_true();
        if (inst.test(x, y) != x + y) {
            return;
        }
        throw new MulibRuntimeException("Must not occur!");
    }

    public int test(int a, int b) { // invokevirtual
        int result = 0;
//        System.out.println("Testing ExSymExeResearch"); HW: Removed to avoid cluttering the console
        if (a >= 0 && a < 100 && b >= 0 && b < 100) {
            int sum = a + b;
            int diff = a - b;
            int temp;

            if (sum > 0) temp = a;
            else temp = b;
            if (temp < diff) throw new MulibRuntimeException("Must not occur!"); // HW: adjusted, this cannot be true for the fixed inputs from Exec
            else result = diff;
        }
        return result;
    }
}