package de.wwu.mulib.transformer.examples;

public class CallsNonSubstitutedToBeConcretized {

    public void check0(double d, int i) {
        NonSubstitutedToBeConcretized n = new NonSubstitutedToBeConcretized();
        n.check0(d, i, 12);
    }

    public int check1(double d0) {
        double d1 = new NonSubstitutedToBeConcretized().check1(12, d0);
        return (int) d0;
    }

    public static double check2(double d0, double d1) {
        return NonSubstitutedToBeConcretized.check2(d0, d1);
    }

    public float check3(int i, double d0, double d1) {
        float f = new NonSubstitutedToBeConcretized().check3(i, d0, d1);
        return f + (float) i;
    }
}
