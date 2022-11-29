package de.wwu.mulib.transformer.examples;

import de.wwu.mulib.Mulib;

public class NonSubstitutedToBeConcretized {

    void check0(double d0, int i, double d1) {

        Mulib.log.info("check" + d0);

    }

    int check1(int i, double d) {


        return 12;
    }

    static double check2(double d0, double d1) {


        return d0 + d1;
    }

    float check3(int i, double d0, double d1) {


        return (float) ((float) i + d0);
    }
}
