package de.wwu.mulib.transformer.examples.free_arrays;

import de.wwu.mulib.Mulib;

public class ArrayIntraMethodTaint {

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
        boolean[] necessarilyTaintedBools = new boolean[14];
        byte[] necessarilyTaintedBytes = new byte[13];
        for (int i = 0; i < taintedBools.length; i++) {
            for (int j = necessarilyTaintedBytes.length - 1; j >= 0; j--) {
                necessarilyTaintedBools[i] = taintedBools[j][i];
                necessarilyTaintedBytes[j] = taintedBytes[10][i];
                necessarilyTaintedBytes[i] = taintedBytes[j][3];
                necessarilyTaintedBools[j] = taintedBools[i][4];
            }
        }
        return 7;
    }

    public int testIntraMethod7(ArrayIntraMethodTaint[] tainted) {
        ArrayIntraMethodTaint[] notNecessarilyTainted = new ArrayIntraMethodTaint[12];
        for (int i = 0; i < notNecessarilyTainted.length; i++) {
            tainted[i] = notNecessarilyTainted[i];
        }
        return 8;
    }

    public int testIntraMethod8(ArrayIntraMethodTaint[][] tainted) {
        ArrayIntraMethodTaint[][] notNecessarilyTainted = new ArrayIntraMethodTaint[12][];
        for (int i = 0; i < tainted.length; i++) {
            for (int j = 0; j < notNecessarilyTainted.length; j++) {
                tainted[i][j] = notNecessarilyTainted[j][i];
            }
        }
        return 9;
    }

    public int testIntraMethod9(ArrayIntraMethodTaint[][] tainted) {
        ArrayIntraMethodTaint[][] necessarilyTainted = new ArrayIntraMethodTaint[12][];
        for (int i = 0; i < tainted.length; i++) {
            tainted[i] = necessarilyTainted[i]; // Must be tainted since we do not wrap Sarrays
        }
        return 10;
    }

    public int testIntraMethod10(ArrayIntraMethodTaint[][] tainted) {
        ArrayIntraMethodTaint[][] necessarilyTainted = new ArrayIntraMethodTaint[12][];
        for (int i = 0; i < tainted.length; i++) {
            for (int j = 0; j < necessarilyTainted.length; j++) {
                necessarilyTainted[j][i] = tainted[i][j];
                necessarilyTainted[i] = tainted[j];
            }
        }
        return 11;
    }

    public int testIntraMethod11(ArrayIntraMethodTaint[][][] tainted) {
        ArrayIntraMethodTaint[][] notNecessarilyTainted = new ArrayIntraMethodTaint[12][];
        for (int i = 0; i < tainted.length; i++) {
            for (int j = 0; j < notNecessarilyTainted.length; j++) {
                tainted[i][j][i] = notNecessarilyTainted[j][i];
            }
        }
        return 12;
    }

    public int testIntraMethod13(ArrayIntraMethodTaint[][] tainted) {
        ArrayIntraMethodTaint[][][] necessarilyTainted = new ArrayIntraMethodTaint[12][][];
        for (int i = 0; i < tainted.length; i++) {
            tainted[i] = necessarilyTainted[i][i]; // Must be tainted since we do not wrap Sarrays
        }
        return 14;
    }

    public int testIntraMethod14(ArrayIntraMethodTaint[][] tainted) {
        ArrayIntraMethodTaint[][][] necessarilyTainted = new ArrayIntraMethodTaint[12][15][];
        for (int i = 0; i < tainted.length; i++) {
            for (int j = 0; j < necessarilyTainted.length; j++) {
                necessarilyTainted[j][i][i] = tainted[i][j];
                necessarilyTainted[i][j] = tainted[j];
            }
        }
        return 15;
    }

    public int testIntraMethod15(int tainted) {
        ArrayIntraMethodTaint[][][] necessarilyTainted = new ArrayIntraMethodTaint[1][][];
        System.out.println(necessarilyTainted[tainted]);
        return 16;
    }

    public int testIntraMethod16(int tainted) {
        ArrayIntraMethodTaint[][][] necessarilyTainted = new ArrayIntraMethodTaint[1][][];
        System.out.println(necessarilyTainted[0][tainted][1]);
        return 17;
    }

    public int testIntraMethod17(int tainted) {
        ArrayIntraMethodTaint[][][] notTainted = new ArrayIntraMethodTaint[1][][];
        ArrayIntraMethodTaint test = notTainted[0][16][1];
        return 18;
    }
}
