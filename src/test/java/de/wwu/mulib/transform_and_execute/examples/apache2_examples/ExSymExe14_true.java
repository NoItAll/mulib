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
import de.wwu.mulib.exceptions.MulibRuntimeException;

// HW: Adapted to fit Mulib, taken from https://github.com/sosy-lab/sv-benchmarks/blob/99d37c5b4072891803b9e5c154127c912477f705/java/jpf-regression/ExSymExe14_true/Main.java
public class ExSymExe14_true {
    static int field;
    static int field2;

    public static void exec() { // HW: Changed from main to exec
        int arg = Mulib.namedFreeShort("arg");
        if (arg < 0) return;
        int x = arg;

        ExSymExe14_true inst = new ExSymExe14_true();
        field = arg;
        inst.test(x, arg, field2);
        // test(x,x);
    }
    /* we want to let the user specify that this method should be symbolic */

    /*
     * test IF_ICMPGT, IADD & ISUB  bytecodes
     */
    public int test(int x, int z, int r) { // HW: Adapted to return int
//        System.out.println("Testing ExSymExe14"); HW: Removed to reduce console clutter
        int y = 3;
        r = x + z;
        x = z - y;
        z = r;
        if (z <= x) {
//            System.out.println("branch FOO1"); HW: Removed to reduce console clutter
            throw new MulibRuntimeException("Must not occur!");
        }// else System.out.println("branch FOO2"); HW: Removed to reduce console clutter
        if (x <= r) return 0; // System.out.println("branch BOO1"); HW: Removed to reduce console clutter
        else return 1;// System.out.println("branch BOO2"); HW: Removed to reduce console clutter
    }
}