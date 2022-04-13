package de.wwu.mulib.transformer.examples;

import de.wwu.mulib.Mulib;

public class HardyRamanujan {

    public static int solve() {
        int a = Mulib.namedFreeInt("a"); int b = Mulib.namedFreeInt("b");
        int c = Mulib.namedFreeInt("c"); int d = Mulib.namedFreeInt("d");
        int e = Mulib.namedFreeInt("e");
        positiveDomain(a,b,c,d,e);
        // Look for a³ + b³ = e = c³ + d³ (bspw. e = 1729)
        if (a != c && a != d && cube(a) + cube(b) == e && cube(c) + cube(d) == e) {
            return e;
        }
        throw Mulib.fail();
    }

    private static void positiveDomain(int... vars) {
        for (int v : vars) {
            if (v <= 0)
                throw Mulib.fail();
        }
    }

    private static int cube(int v) {
        return v * v * v;
    }

}
