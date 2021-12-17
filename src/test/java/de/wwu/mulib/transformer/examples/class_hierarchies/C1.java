package de.wwu.mulib.transformer.examples.class_hierarchies;

public abstract class C1 extends C0 {

    public int i0;

    public int i1;

    @Override
    public int m0() {
        return 1;
    }

    @Override
    public abstract double m1(int i);

    public abstract void m4();

    public int m5(double d) {
        m4();
        return (int) (m0() + m1((int) d));
    }

}
