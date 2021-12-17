package de.wwu.mulib.transform_and_execute.examples;


import de.wwu.mulib.Mulib;

public class BooleanIntChecks {


    public static void exec0() {
        boolean b = Mulib.freeBoolean();
        boolean result = id(b);
        assert !result == !b;
    }

    public static void exec1() {
        boolean b = Mulib.freeBoolean();
        boolean result = id(b);
        assert result == b;
    }

    public static void exec2() {
        boolean b = Mulib.freeBoolean();
        assert id(b) != negate(b);
    }

    public static void exec3() {
        boolean b = Mulib.freeBoolean();
        boolean result = negate(b);
        assert result == !b;
    }

    public static void exec4() {
        boolean b = Mulib.freeBoolean();
        boolean result = negate(b);
        assert result != b;
    }

    public static void exec5() {
        boolean b = Mulib.freeBoolean();
        boolean result = negate(b);
        assert !result == b;
    }

    public static void exec6() {
        boolean b = Mulib.freeBoolean();
        boolean result = negate(b);
        assert !result == (b != (b == (!result == b)));
    }

    public static void exec7() {
        boolean b = Mulib.freeBoolean();
        boolean result = negate(b);
        assert (((!result == b) != b) == !result) == b;
    }

    public static boolean id(boolean b) {
        return b;
    }

    public static boolean negate(boolean b) {
        return !b;
    }
}