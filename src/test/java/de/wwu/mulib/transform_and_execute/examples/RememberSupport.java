package de.wwu.mulib.transform_and_execute.examples;

import de.wwu.mulib.Mulib;

public class RememberSupport {

    public static D check0() {
        D d = new D();
        setupD(d);
        Mulib.remember(d, "d");
        return d;
    }

    public static D check1() {
        D d = new D();
        Mulib.remember(d, "d");
        return d;
    }

    public static D check2(D d) {
        Mulib.remember(d, "d");
        setupD(d);
        return d;
    }

    public static D check3(D d) {
        setupD(d);
        Mulib.remember(d, "d");
        return d;
    }

    public static D check4() {
        D d = Mulib.rememberedFreeObject("d", D.class);
        // Verify that all kinds of values can be remembered
        if (d.val != 200
                || d.a.val != 201) {
            throw Mulib.fail();
        }
        d.a.val = 22;
        return d;
    }

    public static D check5() {
        D d = Mulib.rememberedFreeObject("d", D.class);
        // Verify that all kinds of values can be remembered
        if (d.val != 200
                || d.a.val != 201
                || d.a.b.val != 202) {
            throw Mulib.fail();
        }
        d.a.val = 22;
        return d;
    }

    public static C[][] check6() {
        C[][] c = Mulib.rememberedFreeObject("d", C[][].class);
        if (c[0][0].b.c != c) {
            throw Mulib.fail();
        }
        return c;
    }

    private static void setupD(D d) {
        d.a = new A();
        d.a.b = new B();
        d.a.b.c[0][0] = new C();
        d.a.b.c[0][0].a = new A();
        d.a.b.c[0][0].b = d.a.b;
        d.a.val = 42;
        d.a.b.val = 1337d; // Changed below
        d.a.b.c[0][0].val = true;
        d.a.b.c[0][0].a.val = 43;
        d.a.b.c[0][0].b.val = 1338d;
    }

    public static class A {
        protected B b;
        protected int val;

        public B getB() {
            return b;
        }

        public int getVal() {
            return val;
        }

        public void setVal(int val) {
            this.val = val;
        }
    }

    public static class B {
        protected C[][] c = new C[1][1];
        protected double val;

        public C getC() {
            return c[0][0];
        }

        public C[][] getCAr() {
            return c;
        }

        public double getVal() {
            return val;
        }
    }

    public static class C {
        protected B b;
        protected A a;
        protected boolean val;

        public B getB() {
            return b;
        }

        public A getA() {
            return a;
        }

        public boolean isVal() {
            return val;
        }

        public void setVal(boolean val) {
            this.val = val;
        }
    }

    public static class D {
        protected A a;
        protected char val;

        public A getA() {
            return a;
        }

        public char getVal() {
            return val;
        }

        public void setVal(char c) {
            this.val = c;
        }

        public void setA(A a) {
            this.a = a;
        }
    }
}
