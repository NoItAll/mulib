package de.wwu.mulib.transform_and_execute.examples;

import de.wwu.mulib.Mulib;

public class IntegerScenario {

    public static int checkInit() {
        int val = Mulib.rememberedFreeInt("i");
        Integer i = Integer.valueOf(val);
        if (i < 1) {
            return i;
        }
        Integer i2 = Integer.valueOf(155);
        return i2;
    }

}
