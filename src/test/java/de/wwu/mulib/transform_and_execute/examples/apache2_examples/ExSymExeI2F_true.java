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

import de.wwu.mulib.Mulib;
import de.wwu.mulib.throwables.MulibRuntimeException;

// package gov.nasa.jpf.symbc;
// HW: Adapted to fit Mulib, taken from https://github.com/sosy-lab/sv-benchmarks/blob/99d37c5b4072891803b9e5c154127c912477f705/java/jpf-regression/ExSymExeI2F_true/Main.java
public class ExSymExeI2F_true {

    public static void exec() {
        int x = Mulib.freeBoolean() ? 3 : 0;

        ExSymExeI2F_true inst = new ExSymExeI2F_true();
        inst.test(x);
    }

    public void test(int x) {
        float res = (float) ++x;
        if (res > 0) return; //System.out.println("x >0"); // HW: Removed to reduce console clutter
        else {
            throw new MulibRuntimeException("Must not occur!"); // HW: Adjusted
        }
    }
}