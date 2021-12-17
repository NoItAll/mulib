package de.wwu.mulib.transformer.examples;

public class IntOperations {

    public static void add0(int i0, int i1) {
        int result = i0 + i1;
        return;
    }

    public static int add1(int i0, int i1, int i2) {
        int result = i0 + i1;
        result += i2;
        return result;
    }

    public static int add2(int i0, int i1) {
        return i0 + i1;
    }

    public static int add3(int i0, int i1) {
        int i2 = 3;

        int result = i0 + i1;

        return result + i2;
    }

    public static int add4(int i0, int i2, int i3) {
        return 11 + i0 + 12 + i2;
    }

    public static int add5(int i0, int i2, int i3) {
        return i0 + i2 + 2 + i3 - 5 + 12;
    }

    public static int add6(int i0, int i1, int i2) {
        // i3 & i6 should not be tainted.
        int i3 = 3;
        int i4 = 4;
        i4 = i0 + i3;
        int i5 = i4 + i4;
        int i6 = i3 + i4;
        return i6;
    }

    public static int add7(int i0, int i1) {
        int result = 12 + 27;
        int i3 = i0 + result;
        return result;
    }

    public static int add8() {
        int i0 = 0;
        int i1 = 1;
        return i0 + i1;
    }

    public static int mul0(int i0) {
        return (12+3) * 13 * 14 * 15 * 16 * i0;
    }

    public static int mul1(int i0) {
        return i0 * 12 * 13 * 14;
    }

    public static int diverse0(int i0, int i1) {
        return ((12%i0) / 3) + (((i1-4) + (-48)) * 9);
    }
}
