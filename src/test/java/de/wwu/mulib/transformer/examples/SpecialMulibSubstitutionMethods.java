package de.wwu.mulib.transformer.examples;

import de.wwu.mulib.Mulib;

public class SpecialMulibSubstitutionMethods {

    public int check0() {
        int i = Mulib.freeInt();
        double d = Mulib.freeDouble();
        float f = Mulib.freeFloat();
        boolean b = Mulib.freeBoolean();
        short s = Mulib.freeShort();
        byte by = Mulib.freeByte();
        int j = Mulib.freeInt();
        char c = Mulib.freeChar();

        if (i + j < 100 || b) {
            throw Mulib.fail();
        }
        if (s < by) {
            throw Mulib.fail();
        }
        Mulib.freeLong();
        if (d < f || i < d || f < i && b) {
            throw Mulib.fail();
        }
        long l = Mulib.freeLong();
        if (l < d) {
            throw Mulib.fail();
        }
        if (c < 20) {
            throw Mulib.fail();
        }
        long l2 = Mulib.freeLong();
        return (int) (l + l2);
    }

    public int check1() {
        int i = Mulib.rememberedFreeInt("i");
        double d = Mulib.rememberedFreeDouble("d");
        float f = Mulib.rememberedFreeFloat("f");
        boolean b = Mulib.rememberedFreeBoolean("b");
        short s = Mulib.rememberedFreeShort("s");
        byte by = Mulib.rememberedFreeByte("by");
        int j = Mulib.rememberedFreeInt("j");
        char c = Mulib.rememberedFreeChar("c");

        if (i + j < 100) {
            throw Mulib.fail();
        }
        if (s < by || b) {
            throw Mulib.fail();
        }

        Mulib.rememberedFreeLong("null");
        if (d < f || i < d || f < i && b) {
            throw Mulib.fail();
        }
        Mulib.rememberedFreeLong(null);

        long l = Mulib.rememberedFreeLong("l");
        if (l < d) {
            throw Mulib.fail();
        }
        if (c > 20) {
            throw Mulib.fail();
        }
        long l2 = Mulib.rememberedFreeLong("l");
        return (int) (l + l2);
    }

    public int checkAssume() {
        int i = Mulib.rememberedFreeInt("i");
        double d = Mulib.rememberedFreeDouble("d");
        float f = Mulib.rememberedFreeFloat("f");
        boolean b = Mulib.rememberedFreeBoolean("b");
        short s = Mulib.rememberedFreeShort("s");
        byte by = Mulib.rememberedFreeByte("by");
        long j = Mulib.rememberedFreeLong("j");
        char c = Mulib.rememberedFreeChar("c");
        if (i <= d) {
            Mulib.assume(d >= i);
        } else if (c < by) {
            Mulib.assume(c == s);
        } else if (j > c) {
            Mulib.assume(b);
        } else if (f == c) {
            Mulib.assume(by > j);
        }
        return (int) (c + j);
    }

    public int checkCheck() {
        int i = Mulib.rememberedFreeInt("i");
        double d = Mulib.rememberedFreeDouble("d");
        float f = Mulib.rememberedFreeFloat("f");
        boolean b = Mulib.rememberedFreeBoolean("b");
        short s = Mulib.rememberedFreeShort("s");
        byte by = Mulib.rememberedFreeByte("by");
        long j = Mulib.rememberedFreeLong("j");
        char c = Mulib.rememberedFreeChar("c");
        if (Mulib.check(i <= d)) {
            return 0;
        } else if (Mulib.check(c < by)) {
            return 1;
        } else if (Mulib.check(j > c)) {
            Mulib.assume(b);
        } else if (Mulib.check(i == j)) {
            return 2;
        }
        return 17;
    }

    public int checkCheckAssume() {
        int i = Mulib.rememberedFreeInt("i");
        double d = Mulib.rememberedFreeDouble("d");
        float f = Mulib.rememberedFreeFloat("f");
        boolean b = Mulib.rememberedFreeBoolean("b");
        short s = Mulib.rememberedFreeShort("s");
        byte by = Mulib.rememberedFreeByte("by");
        long j = Mulib.rememberedFreeLong("j");
        char c = Mulib.rememberedFreeChar("c");
        if (Mulib.checkAssume(i <= d)) {
            return 0;
        } else if (Mulib.checkAssume(c < by)) {
            return 1;
        } else if (Mulib.checkAssume(j > c)) {
            Mulib.assume(b);
        } else if (Mulib.checkAssume(f == c)) {
            return s;
        }
        return (int) (c + j);
    }

    public int checkIsInSearch() {
        return Mulib.isInSearch() ? 1 : 5;
    }

}
