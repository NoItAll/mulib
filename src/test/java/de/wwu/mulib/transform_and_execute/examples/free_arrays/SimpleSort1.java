package de.wwu.mulib.transform_and_execute.examples.free_arrays;

import de.wwu.mulib.Mulib;

public class SimpleSort1 {

    public static int[] sort(int[] bBeforeFree) {
        int n = bBeforeFree.length;
        int[] b = Mulib.freeIntArray();
        int[] idx = Mulib.freeIntArray();
        boolean failed = false;
        for (int i = 0; i < n; i++) {
            if (idx[i] < 0 || idx[i] >= n) {
                failed = true; break;
            }
            boolean innerFailed = false;
            for (int j = 0; j < n; j++) {
                if (i != j && idx[i] == idx[j]) {
                    innerFailed = true; break;
                }
            }
            if (innerFailed) {
                failed = true; break;
            }
        }
        if (failed) {
            throw Mulib.fail();
        }
        for (int i = 0; i < n; i++) {
            b[i] = bBeforeFree[i];
        }
        int[] a = Mulib.freeIntArray();
        for (int i = 0; i < n; i++) {
            a[idx[i]] = b[i];
        }

        for (int i = 0; i < n-1; i++) {
            if (a[i] > a[i+1]) {
                throw Mulib.fail();
            }
        }
        return a;
    }


}
