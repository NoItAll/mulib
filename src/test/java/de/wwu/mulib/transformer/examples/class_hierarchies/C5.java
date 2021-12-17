package de.wwu.mulib.transformer.examples.class_hierarchies;

public class C5 extends C1 implements I2 {

    @Override
    public void m2(double d, int i) {
        C3 t = new C3();
    }

    @Override
    public double m1(int i) {
        return 0;
    }

    public void m3() {}

    @Override
    public void m4() {
        m0();
        m1(2);
        m2(3.1, 4);
        m4();
        m5(12);
    }

    @Override
    public C1 c0() {
        return null;
    }
}
