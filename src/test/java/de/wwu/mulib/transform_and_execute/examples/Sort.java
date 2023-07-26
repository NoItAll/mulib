package de.wwu.mulib.transform_and_execute.examples;

import de.wwu.mulib.Mulib;

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

/* 2021-11-14 Taken and adjusted from https://github.com/sosy-lab/sv-benchmarks/blob/master/java/algorithms/InsertionSort-FunSat01/Main.java
    : LT */
/* 2023-07-26 Taken as an example to integrate Mulib with Dacite (https://github.com/dacite-defuse/DynamicDefUse/) : HW */

public class Sort {

  public int[] sort(int[] a) {
    final int N = a.length;
    for (int i = 1; i < N; i++) { // N branches
      int j = i - 1;
      int x = a[i];
      // First branch (j >= 0):  2 + 3 + ... + N = N(N+1)/2 - 1 branches
      // Second branch (a[j] > x):  1 + 2 + ... + N-1 = (N-1)N/2 branches
      while ((j >= 0) && (a[j] > x)) {
        a[j + 1] = a[j];
        j=j-1;
      }
      a[j + 1] = x;
    }
    return a;
  }

  public static int[] sort() {
    int[] a = Mulib.rememberedFreeObject("input", int[].class);
    return new Sort().sort(a);
  }

}
