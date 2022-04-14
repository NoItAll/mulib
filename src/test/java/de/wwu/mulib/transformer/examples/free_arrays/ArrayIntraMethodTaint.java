package de.wwu.mulib.transformer.examples.free_arrays;

import de.wwu.mulib.Mulib;

public class ArrayIntraMethodTaint {
    /// TODO

    public static int testIntraMethod0(double d, int[] taintedAr) {
        int[] notTainted = new int[12];
        if (taintedAr.length < 12) {
            throw Mulib.fail();
        }
        for (int i = 0; i < notTainted.length; i++) {
            taintedAr[i] = notTainted[i];
        }
        return 1;
    }

    public static int testIntraMethod1(int[] taintedAr, int check) {
        int[] notTainted = new int[12];
        if (taintedAr.length < 12) {
            throw Mulib.fail();
        }
        for (int i = 0; i < taintedAr.length; i++) {
            if (check < taintedAr[i]) {
                taintedAr[i] = notTainted[i];
            }
        }
        return 1;
    }

    public static int testIntraMethod2(int[] taintedAr, int check) {
        int[] tainted = new int[12];
        if (taintedAr.length < 12) {
            throw Mulib.fail();
        }
        for (int i = 0; i < tainted.length; i++) {
            if (check < taintedAr[i]) {
                tainted[i] = taintedAr[i];
            }
        }
        return 1;
    }

    public static int testIntraMethod3(boolean[] taintedBools, byte[] taintedBytes) {
        boolean[] notTaintedBools = new boolean[14];
        byte[] notTaintedBytes = new byte[13];
        for (int i = 0; i < taintedBools.length; i++) {
            for (int j = notTaintedBytes.length - 1; j >= 0; j--) {
                taintedBools[i] = notTaintedBools[j];
                taintedBytes[j] = notTaintedBytes[j];
                taintedBytes[i] = notTaintedBytes[i];
                taintedBools[j] = notTaintedBools[i];
            }
        }
        return 4;
    }

    public static int testIntraMethod4(byte[] taintedBytes, boolean[] taintedBools) {
        boolean[] alsoTaintedBools = new boolean[14];
        byte[] alsoTaintedBytes = new byte[13];
        for (int i = 0; i < alsoTaintedBools.length; i++) {
            for (int j = alsoTaintedBytes.length - 1; j >= 0; j--) {
                alsoTaintedBools[i] = alsoTaintedBools[j];
                alsoTaintedBytes[j] = alsoTaintedBytes[j];
                alsoTaintedBytes[i] = alsoTaintedBytes[i];
                alsoTaintedBools[j] = alsoTaintedBools[i];
            }
        }
        return 5;
    }

    public static int testIntraMethod5(boolean[] taintedBools, byte[] taintedBytes) {
        boolean[][] notTaintedBools = new boolean[14][];
        byte[][] notTaintedBytes = new byte[13][];
        for (int i = 0; i < taintedBools.length; i++) {
            for (int j = notTaintedBytes.length - 1; j >= 0; j--) {
                taintedBools[i] = notTaintedBools[j][i];
                taintedBytes[j] = notTaintedBytes[j][j];
                taintedBytes[i] = notTaintedBytes[i][j];
                taintedBools[i] = notTaintedBools[i][i];
            }
        }
        return 6;
    }

    public static int testIntraMethod6(byte[][] taintedBytes, boolean[][] taintedBools) {
        boolean[] TaintedBools = new boolean[14];
        byte[] TaintedBytes = new byte[13];
        for (int i = 0; i < taintedBools.length; i++) {
            for (int j = TaintedBytes.length - 1; j >= 0; j--) {
                TaintedBools[i] = taintedBools[j][i];
                TaintedBytes[j] = taintedBytes[10][i];
                TaintedBytes[i] = taintedBytes[j][3];
                TaintedBools[j] = taintedBools[i][4];
            }
        }
        return 7;
    }
}
