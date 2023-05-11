package de.wwu.mulib.transform_and_execute.examples;

import de.wwu.mulib.Mulib;

public class AliasingAndIdsEq {

    public static ResultContainer objectsEq() {
        A a0 = Mulib.freeObject(A.class);
        A a1 = Mulib.freeObject(A.class);
        A a2 = Mulib.pickFrom(a0, a1);
        if (a1 == a0) {
            if (a0.i != 42) {
                throw Mulib.fail();
            }
            if (a1.i != 42) {
                return null; // Values a0.i and a1.i must equal 42
            }
            return new ResultContainer(a1, a0, 1);
        } else if (a2 == a0) {
            if (a2 == a1) {
                return null; // Since a1 != a0 && a2 == a0: a2 != a1 should hold
            }
            if (a2.i != 42) {
                throw Mulib.fail();
            }
            if (a0.i != 42) {
                return null; // Values a0.i and a2.i must equal 42
            }
            return new ResultContainer(a2, a0, 2);
        } else {
            if (a2.i != a1.i) {
                return null; // Values a2.i and a1.i must equal
            }
            if (a2 != a1) {
                return null; // Identity must equal
            }
            return new ResultContainer(a2, a1, 3);
        }
    }

    public static class A {
        int i = 0;
    }

    public static class ResultContainer {
        private final A aliasingObject;
        private final A aliasingTarget;
        private final byte resultIndicator;
        public ResultContainer(
                A aliasingObject,
                A aliasingTarget,
                int resultIndicator) {
            this.aliasingObject = aliasingObject;
            this.aliasingTarget = aliasingTarget;
            this.resultIndicator = (byte) resultIndicator;
        }

        public A getAliasingObject() {
            return aliasingObject;
        }

        public A getAliasingTarget() {
            return aliasingTarget;
        }

        public byte getResultIndicator() {
            return resultIndicator;
        }
    }
}
