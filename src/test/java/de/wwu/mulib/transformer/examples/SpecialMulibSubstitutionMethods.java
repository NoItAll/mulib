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
        long l2 = Mulib.rememberedFreeLong("l");
        return (int) (l + l2);
    }

}
