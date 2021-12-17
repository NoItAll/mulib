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

// HW: Adapted to fit Mulib, taken from https://github.com/sosy-lab/sv-benchmarks/blob/99d37c5b4072891803b9e5c154127c912477f705/java/jpf-regression/ExSymExeLCMP_false/Main.java
public class ExSymExeLCMP_false {

    public static void exec0() { // HW: changed from main to exec
        long x = Mulib.freeLong();

        ExSymExeLCMP_false inst = new ExSymExeLCMP_false();
        inst.test(x, 5);
    }

    public static void exec1() { // HW: Added
        long x = Mulib.freeLong();

        ExSymExeLCMP_false inst = new ExSymExeLCMP_false();
        inst.otherTest(x, Mulib.freeInt());
    }

    public boolean otherTest(long x, long y) { // HW: added to also account for true case

        long res = x;
        if (res + 1 > y + 2) return true; // HW: y instead of res
        else {
            assert false;
            return false;
        }
    }

    public boolean test(long x, long y) { // HW: changed return to boolean

        long res = x;
        if (res + 1 > res + 2) return true;// System.out.println("x >0"); HW: Removed
        else {
            assert false;
            return false;
//            System.out.println("x <=0"); HW: Removed
        }
    }
}