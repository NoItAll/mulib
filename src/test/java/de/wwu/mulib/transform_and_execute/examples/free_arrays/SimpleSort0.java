package de.wwu.mulib.transform_and_execute.examples.free_arrays;

import de.wwu.mulib.Mulib;

public class SimpleSort0 {

    public static int[] sort(int[] b) {
        int[] idx = Mulib.freeIntArray();
        boolean[] usedIdx = Mulib.freeBooleanArray();
        int[] a = Mulib.freeIntArray();

        if (a.length != b.length) {
            throw Mulib.fail();
        }

        for (int i = 0; i < b.length; i++) {
            if (usedIdx[idx[i]]) {
                throw Mulib.fail();
            }
            a[idx[i]] = b[i];
            usedIdx[idx[i]] = true;
        }

        for (int i = 0; i < b.length - 1; i++) {
            if (a[i] > a[i+1]) {
                throw Mulib.fail();
            }
        }
        return a;
    }


}
