package de.wwu.mulib.transformer.examples.class_hierarchies;

public class C4 extends C3 implements I1, I4 {
    @Override
    public C5 c0() {
        return new C5();
    }

    @Override
    public C2 c2() {
        return null;
    }

    public C3 c3(I1 i1) {
        return (C3) (C0) i1.c0();
    }
}
