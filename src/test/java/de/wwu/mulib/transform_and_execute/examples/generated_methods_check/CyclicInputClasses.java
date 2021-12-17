package de.wwu.mulib.transform_and_execute.examples.generated_methods_check;

public class CyclicInputClasses {
    private short s;
    private final CyclicInputClasses cyclic;

    public CyclicInputClasses() {
        cyclic = new CyclicInputClasses(this, (short) -1);
        s = 42;
    }

    public CyclicInputClasses(CyclicInputClasses c, short s) {
        this.cyclic = c;
        this.s = s;
    }

    public static CyclicInputClasses calc(CyclicInputClasses c0, CyclicInputClasses c1) {
        short s = (short) (c0.s + c1.s);
        return new CyclicInputClasses(c0.s > c1.s ? c0 : c1, s);
    }

    public short getS() {
        return s;
    }

    public final CyclicInputClasses getCyclic() {
        return cyclic;
    }
}
