package de.wwu.mulib.transformer.examples;

public class IndexChanges0 {

    public static long m0(long l) {
        int i0 = 42;
        l += i0;
        return l;
    }

    public static long m1(long l, double d) {
        int i0 = 42;
        int i1 = 43;
        int i2 = 14;
        l += i0 + i1 + i2 + (int) d;
        return l;
    }

    public static long m2(long l, double d) {
        int i0 = 42;
        int i1 = 43;
        int i2 = 14;
        l += i0 + i1 + (int) d + i2;
        return l;
    }

    public static long m3(long l, double d) {
        int i0 = 42;
        int i1 = 43;
        int i2 = 14;
        l += i0 + (int) d + i1 + i2;
        return l;
    }

    public static long m4(long l) {
        if (l < 0) {
            long l0 = (int) l;
            l += l0;
        } else {
            int i0 = 42;
            int i1 = 43;
            l += i0 + i1;
        }
        return l;
    }
}
