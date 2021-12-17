package de.wwu.mulib.transform_and_execute.examples;

import de.wwu.mulib.Mulib;

public class BoolCounterTransf {

    public static int count4() {
        boolean b0 = Mulib.freeBoolean();
        boolean b1 = Mulib.freeBoolean();
        boolean b2 = Mulib.freeBoolean();
        boolean b3 = Mulib.freeBoolean();
        int result = 0;
        if (b0)
            result += 1;
        if (b1)
            result += 2;
        if (b2)
            result += 4;
        if (b3)
            result += 8;
        return result;
    }

}
