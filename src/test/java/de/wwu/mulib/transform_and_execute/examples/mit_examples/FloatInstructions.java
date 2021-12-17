package de.wwu.mulib.transform_and_execute.examples.mit_examples;

/*
 * Contributed to SV-COMP by Falk Howar
 * License: MIT (see /java/jdart-regression/LICENSE-MIT)
 *
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

// HW: Adapted to fit Mulib, taken from https://github.com/sosy-lab/sv-benchmarks/blob/99d37c5b4072891803b9e5c154127c912477f705/java/jdart-regression/float/Main.java
public class FloatInstructions {

    public static void exec() {
        float a = Mulib.freeFloat();
        float b = Mulib.namedFreeFloat("b");
        if (a + b != a || b == 0.0) {
            return;
        }
        throw new MulibRuntimeException("Must not occur!");
    }
}