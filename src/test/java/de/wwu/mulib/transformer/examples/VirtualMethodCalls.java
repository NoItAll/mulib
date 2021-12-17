package de.wwu.mulib.transformer.examples;

public class VirtualMethodCalls {

    public VirtualMethodCalls() {}

     // Checks public virtual method call
    public final synchronized void virtualMethodCall0() {
        System.out.println(42);
    }

    // Check protected virtual method call with parameters
    protected int virtualMethodCall1(int i, double d, boolean b) {
        if (b) {
            return 42;
        } else if (i == (int) d) {
            return (int) d;
        } else {
            return i;
        }
    }

    // Check private virtual method call with parameters
    private double virtualMethodCall2(int i, double d, boolean b) {
        if (b) {
            return 42;
        } else if (i == (int) d) {
            return d;
        } else {
            return i;
        }
    }

    // Check package private virtual method call with parameters
    double virtualMethodCall3(double d, int i, boolean b) {
        if (b) {
            return (float) 42.0;
        } else if (i == (int) d) {
            return (int) d;
        } else {
            return i;
        }
    }

    protected static boolean staticMethodCall0(double d, int i, boolean b) {
        return d == (double) i && b;
    }

    public double virtualMethodCall4(int i, double d, boolean b) {
        virtualMethodCall0();
        boolean nb = !b;
        virtualMethodCall1(i, d, nb);
        nb = staticMethodCall0(d, i, nb);
        d = virtualMethodCall2(i, d, b);
        d = virtualMethodCall3(d, i, nb);
        return d;
    }

    public double virtualMethodCall5(int i, double d, boolean b) {
        boolean nb0 = !b;
        boolean nb1 = !nb0;
        return d;
    }

    public boolean virtualMethodCall6(int i, double d, boolean b) {
        virtualMethodCall0();
        boolean nb0 = !b;
        virtualMethodCall1(i, d, nb0);
        nb0 = staticMethodCall0(d, i, b);
        boolean nb1 = !nb0;
        return nb1;
    }

    public boolean virtualMethodCall7(int i, boolean b, double d) {
        virtualMethodCall0();
        boolean nb0 = !b;
        virtualMethodCall1(i, d, nb0);
        boolean nb1 = !nb0;
        return nb1;
    }

    public boolean virtualMethodCall7(double d0, boolean b, double d1) {
        virtualMethodCall0();
        boolean nb0 = staticMethodCall0(d0, (int) d1, b);
        return nb0;
    }

    public boolean virtualMethodCall8(double d0, boolean b, double d1) {
        virtualMethodCall0();
        boolean nb0 = staticMethodCall0(d0, (int) d1, b);
        return nb0;
    }

    public boolean virtualMethodCall9() {
        boolean nb0 = virtualMethodCall8(0, false, 5.0);
        return nb0;
    }
}
