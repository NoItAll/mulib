package de.wwu.mulib.transformer.examples;

import de.wwu.mulib.Mulib;

public class SpecialMulibSubstitutionMethods {

    public void check0() {
        int i = Mulib.freeInt();
        double d = Mulib.freeDouble();
        float f = Mulib.freeFloat();
        boolean b = Mulib.freeBoolean();
        int j = 42;

        if (i + j < 100 || b) {
            throw Mulib.fail();
        }
        if (d < f || i < d || f < i && b) {
            throw Mulib.fail();
        }
    }

}
