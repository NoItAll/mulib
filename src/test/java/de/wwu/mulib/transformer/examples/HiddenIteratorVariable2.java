package de.wwu.mulib.transformer.examples;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.transformer.examples.class_hierarchies.C0;

import java.util.List;

public class HiddenIteratorVariable2 {

    public void iterate0(List<C0> list, double d) {
        for (C0 element : list) {
            element.m0();
        }
    }

    public void iterate1(long l, double d0, List<C0> list, double d1) {
        for (C0 element : list) {
            element.m0();
        }
    }

    public void iterate2(long l, double d0, List<C0> list, double d1) {
        for (C0 element : list) {
            for (C0 elementInner : list) {
                element.m0();
                elementInner.m0();
            }
        }
    }

    public void iterate3(long l00, List<C0> list, long l01) {
        int i0 = (int) l00;
        long l0 = 35;
        for (C0 element : list) {
            long l1 = l01;
            int i1 = 12;
            for (C0 elementInner : list) {
                long l2 = (long) l01;
                int i2 = 123123;
                element.m0();
                long l3 = l1 + l2;
                elementInner.m0();
            }
        }

        for (C0 element : list) {
            long l1 = l01;
            int i1 = 12;
            for (C0 elementInner : list) {
                long l2 = (long) l01;
                int i2 = 123123;
                element.m0();
                long l3 = l1 + l2;
                elementInner.m0();
            }
        }
    }

    public void iterate4(List<C0> list) {
        for (C0 element : list) {
            for (C0 elementInner : list) {
                for (C0 elementInnerInner : list) {
                }
                double d = Mulib.freeDouble();
                long l = Mulib.freeLong();
            }
        }
    }

    public void iterate5(List<C0> list) {
        for (C0 element : list) {
            for (C0 elementInner : list) {
                for (C0 elementInnerInner : list) {
                }
                short s1 = Mulib.freeShort();
            }
        }
    }

    public void iterate6(List<C0> list) {
        for (C0 element : list) {
        }
        long l = Mulib.freeLong();
    }

    public void iterate7(List<C0> list) {
        for (C0 element : list) {
            element.m0();
        }
        long l = Mulib.freeLong();
    }

    public void iterate8(long l0, List<C0> list, double d0) {
        for (C0 element : list) {
            long l1 = Mulib.freeInt();
            int i1 = 12;
            for (C0 elementInner : list) {
                long l2 = Mulib.freeByte();
                for (C0 elementInnerInner : list) {
                    double d1 = 123123;
                    element.m0();
                    elementInnerInner.m2(d1, (int) l1);
                }
                long l3 = (long) d0;
                elementInner.m0();
                short s1 = Mulib.freeShort();
            }
        }
    }
}
