package de.wwu.mulib.transformer.examples;

import de.wwu.mulib.Mulib;

public class HardyRamanujan {

    public static int solve() {
        int a = Mulib.rememberedFreeInt("a"); int b = Mulib.rememberedFreeInt("b");
        int c = Mulib.rememberedFreeInt("c"); int d = Mulib.rememberedFreeInt("d");
        int e = Mulib.rememberedFreeInt("e");
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
