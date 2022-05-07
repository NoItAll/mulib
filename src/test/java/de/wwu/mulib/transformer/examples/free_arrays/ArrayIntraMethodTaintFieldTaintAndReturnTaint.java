package de.wwu.mulib.transformer.examples.free_arrays;

import de.wwu.mulib.Mulib;

public class ArrayIntraMethodTaintFieldTaintAndReturnTaint {
    private int[] intAr;
    protected double[] doubleAr;
    public float[] floatAr;
    long[] longAr;
    short[][] shortArs;
    private byte[][] byteArs;
    private boolean[][][] booleanArs;
    ArrayIntraMethodTaintFieldTaintAndReturnTaint[] objectAr0;
    private ArrayIntraMethodTaintFieldTaintAndReturnTaint[][] objectAr1;

    public int testIntraMethodTaintAndReturn0(byte[][] byteArs) {
        int[] notTainted = new int[12];
        if (intAr.length < 12) {
            throw Mulib.fail();
        }
        for (int i = 0; i < notTainted.length; i++) {
            intAr[i] = notTainted[i] + byteArs[i][i];
        }
        return 1;
    }

    public int testIntraMethodTaintAndReturn1(int check) {
        int[] notTainted = new int[12];
        if (doubleAr.length < 12) {
            throw Mulib.fail();
        }
        for (int i = 0; i < doubleAr.length; i++) {
            if (check < doubleAr[i]) {
                doubleAr[i] = notTainted[i];
            }
        }
        return 1;
    }

    public float[] testIntraMethodTaintAndReturn2(float check) {
        float[] tainted = new float[12];
        for (int i = 0; i < longAr.length; i++) {
            if (check < longAr[i]) {
                byteArs[i][i] = (byte) longAr[i];
            }
            tainted[i] = check;
        }
        return tainted;
    }

    public boolean[][] testIntraMethodTaintAndReturn3(int check) {
        boolean[] notTaintedBools = new boolean[14];
        byte[] notTaintedBytes = new byte[13];
        for (int i = 0; i < booleanArs.length; i++) {
            for (int j = notTaintedBytes.length - 1; j >= 0; j--) {
                booleanArs[i][i][i] = notTaintedBools[j];
                byteArs[j][j] = notTaintedBytes[j];
                byteArs[i][j] = notTaintedBytes[i];
                booleanArs[j][i][i] = notTaintedBools[i];
            }
        }
        return booleanArs[check];
    }

    public boolean[] testIntraMethodTaintAndReturn4() {
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
        return alsoTaintedBools;
    }

    public byte[] testIntraMethodTaintAndReturn5(int check) {
        boolean[][] notTaintedBools = new boolean[14][];
        byte[][] taintedBytes = new byte[13][];
        for (int i = 0; i < booleanArs.length; i++) {
            for (int j = taintedBytes.length - 1; j >= 0; j--) {
                booleanArs[i][j][i] = notTaintedBools[j][i];
                byteArs[j][i] = taintedBytes[j][j];
                byteArs[i] = taintedBytes[j];
                booleanArs[i][i] = notTaintedBools[i];
            }
        }
        return taintedBytes[check];
    }

    public boolean[] testIntraMethodTaintAndReturn6() {
        boolean[] taintedBools = new boolean[14];
        byte[] necessarilyTaintedBytes = new byte[13];
        for (int i = 0; i < taintedBools.length; i++) {
            for (int j = necessarilyTaintedBytes.length - 1; j >= 0; j--) {
                taintedBools[i] = booleanArs[j][i][j];
                necessarilyTaintedBytes[j] = byteArs[10][i];
                necessarilyTaintedBytes[i] = byteArs[j][3];
                taintedBools = booleanArs[i][4];
            }
        }
        return taintedBools;
    }

    public int testIntraMethodTaintAndReturn7() {
        ArrayIntraMethodTaintFieldTaintAndReturnTaint[] notNecessarilyTainted = new ArrayIntraMethodTaintFieldTaintAndReturnTaint[12];
        for (int i = 0; i < notNecessarilyTainted.length; i++) {
            objectAr0[i] = notNecessarilyTainted[i];
        }
        return 8;
    }

    public ArrayIntraMethodTaintFieldTaintAndReturnTaint[] testIntraMethodTaintAndReturn8(ArrayIntraMethodTaintFieldTaintAndReturnTaint[][] tainted, float check) {
        ArrayIntraMethodTaintFieldTaintAndReturnTaint[][] alsoTainted = new ArrayIntraMethodTaintFieldTaintAndReturnTaint[12][];
        for (int i = 0; i < tainted.length; i++) {
            for (int j = 0; j < alsoTainted.length; j++) {
                tainted[i][j] = alsoTainted[j][i];
                alsoTainted[i] = objectAr0;
                tainted[j][i] = objectAr1[i][j];
                alsoTainted[i] = objectAr1[i];
                alsoTainted = objectAr1;
                objectAr1 = tainted;
            }
        }
        return tainted[(int) check];
    }

    public ArrayIntraMethodTaintFieldTaintAndReturnTaint testIntraMethodTaintAndReturn9(ArrayIntraMethodTaintFieldTaintAndReturnTaint[][] tainted) {
        ArrayIntraMethodTaintFieldTaintAndReturnTaint[][] necessarilyTainted = new ArrayIntraMethodTaintFieldTaintAndReturnTaint[12][];
        return necessarilyTainted[0][0];
    }

    public ArrayIntraMethodTaintFieldTaintAndReturnTaint[] testIntraMethodTaintAndReturn10(ArrayIntraMethodTaintFieldTaintAndReturnTaint[][] tainted) {
        ArrayIntraMethodTaintFieldTaintAndReturnTaint[][] necessarilyTainted = new ArrayIntraMethodTaintFieldTaintAndReturnTaint[12][];
        return necessarilyTainted[0];
    }

    public int testIntraMethodTaintAndReturn11(ArrayIntraMethodTaintFieldTaintAndReturnTaint[][][] tainted) {
        ArrayIntraMethodTaintFieldTaintAndReturnTaint[][] notNecessarilyTainted = new ArrayIntraMethodTaintFieldTaintAndReturnTaint[12][];
        for (int i = 0; i < tainted.length; i++) {
            for (int j = 0; j < notNecessarilyTainted.length; j++) {
                tainted[i][j][i] = notNecessarilyTainted[j][i];
                objectAr1 = tainted[i];
                objectAr1[i] = tainted[i][j];
                objectAr1[i][j] = tainted[j][j][i*j];
                objectAr0 = tainted[i][i];
                objectAr0[i] = tainted[j][i+j+i+(2*tainted.length)][j];
                objectAr0[i] = notNecessarilyTainted[(i * 3 + notNecessarilyTainted.length)/6][j];
            }
        }
        return 12;
    }

    public int testIntraMethodTaintAndReturn13(ArrayIntraMethodTaintFieldTaintAndReturnTaint[][] tainted) {
        ArrayIntraMethodTaintFieldTaintAndReturnTaint[][][] necessarilyTainted = new ArrayIntraMethodTaintFieldTaintAndReturnTaint[12][][];
        for (int i = 0; i < tainted.length; i++) {
            tainted[i] = necessarilyTainted[i][i]; // Must be tainted since we do not wrap Sarrays
        }
        return 14;
    }

    public ArrayIntraMethodTaintFieldTaintAndReturnTaint[][] testIntraMethodTaintAndReturn14(ArrayIntraMethodTaintFieldTaintAndReturnTaint[][][] tainted) {
        ArrayIntraMethodTaintFieldTaintAndReturnTaint[][] necessarilyTainted = new ArrayIntraMethodTaintFieldTaintAndReturnTaint[12][];
        for (int i = 0; i < tainted.length; i++) {
            for (int j = 0; j < necessarilyTainted.length; j++) {
                tainted[i][j][i] = necessarilyTainted[j][i];
                objectAr1 = tainted[i];
                objectAr1[i] = tainted[i][j];
                objectAr1[i][j] = tainted[j][j][i*j];
                objectAr0 = tainted[i][i];
                objectAr0[i] = tainted[j][i+j+i+(2*tainted.length)][j];
                objectAr0[i] = necessarilyTainted[(i * 3 + necessarilyTainted.length)/6][j];
            }
        }
        return necessarilyTainted;
    }

    public ArrayIntraMethodTaintFieldTaintAndReturnTaint[] testIntraMethodTaintAndReturn15(ArrayIntraMethodTaintFieldTaintAndReturnTaint[][][] tainted) {
        ArrayIntraMethodTaintFieldTaintAndReturnTaint[][] necessarilyTainted = new ArrayIntraMethodTaintFieldTaintAndReturnTaint[12][];
        for (int i = 0; i < tainted.length; i++) {
            for (int j = 0; j < necessarilyTainted.length; j++) {
                tainted[i][j][i] = necessarilyTainted[j][i];
                objectAr1 = tainted[i];
                objectAr1[i] = tainted[i][j];
                objectAr1[i][j] = tainted[j][j][i*j];
                objectAr0 = tainted[i][i];
                objectAr0[i] = tainted[j][i+j+i+(2*tainted.length)][j];
                objectAr0[i] = necessarilyTainted[(i * 3 + necessarilyTainted.length)/6][j];
            }
        }
        return necessarilyTainted[0];
    }
}
