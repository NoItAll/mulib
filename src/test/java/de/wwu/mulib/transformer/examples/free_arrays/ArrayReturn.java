package de.wwu.mulib.transformer.examples.free_arrays;

import de.wwu.mulib.Mulib;

public class ArrayReturn {
    public static int[] arReturn0() {
        return Mulib.freeIntArray();
    }

    public static long[] arReturn1() {
        return Mulib.freeLongArray();
    }

    public static double[] arReturn2() {
        return Mulib.freeDoubleArray();
    }

    public static float[] arReturn3() {
        return Mulib.freeFloatArray();
    }

    public static short[] arReturn4() {
        return Mulib.freeShortArray();
    }

    public static byte[] arReturn5() {
        return Mulib.freeByteArray();
    }

    public static boolean[] arReturn6() {
        return Mulib.freeBooleanArray();
    }

    public static ArrayReturn[][] arReturn7() {
        return Mulib.freeObject(ArrayReturn[][].class);
    }

    public static ArrayReturn[] arReturn8() {
        return Mulib.freeObject(ArrayReturn[].class);
    }

    public static int[] arReturn10() {
        return Mulib.namedFreeIntArray("name");
    }

    public static long[] arReturn11() {
        return Mulib.namedFreeLongArray("name");
    }

    public static double[] arReturn12() {
        return Mulib.namedFreeDoubleArray("name");
    }

    public static float[] arReturn13() {
        return Mulib.namedFreeFloatArray("name");
    }

    public static short[] arReturn14() {
        return Mulib.namedFreeShortArray("name");
    }

    public static byte[] arReturn15() {
        return Mulib.namedFreeByteArray("name");
    }

    public static boolean[] arReturn16() {
        return Mulib.namedFreeBooleanArray("name");
    }

    public static ArrayReturn[][][] arReturn17() {
        return Mulib.namedFreeObject("name", ArrayReturn[][][].class);
    }

    public static ArrayReturn[] arReturn18() {
        return Mulib.namedFreeObject("name", ArrayReturn[].class);
    }

    public static int[] arReturn20() {
        return new int[0];
    }

    public static long[] arReturn21() {
        return new long[129];
    }

    public static double[] arReturn22() {
        return new double[] {1.0, 2.0};
    }

    public static float[] arReturn23() {
        float[] result = new float[3];
        result[0] = 3.4f;
        result[1] = (float) 1.4;
        result[2] = (int) 33d;
        return result;
    }

    public static short[] arReturn24() {
        return null;
    }

    public static byte[] arReturn25() {
        return new byte[-9];
    }

    public static boolean[] arReturn26() {
        return new boolean[1000000];
    }

    public static ArrayReturn[] arReturn27() {
        ArrayReturn arrayReturn = new ArrayReturn();
        ArrayReturn[] result = new ArrayReturn[] { null, new ArrayReturn(), arrayReturn};
        return result;
    }

    public static ArrayReturn[] arReturn28() {
        ArrayReturn arrayReturn = new ArrayReturn();
        return new ArrayReturn[] { null, new ArrayReturn(), arrayReturn};
    }

    public static int[][] arReturn29() {
        int[][] i = new int[5][12];
        return i;
    }

    public static int[][] arReturn30() {
        int[][] i = new int[5][12];
        i[4][3] = 2;
        return i;
    }

    public static boolean[][] arReturn31() {
        boolean[][] b = new boolean[100][12];
        b[4][3] = true;
        b[73][2] = b[3][4];
        b[2] = b[5];
        return b;
    }

    public static byte[][] arReturn32() {
        byte[][] b = new byte[12][12];
        b[4][3] = 1;
        b[1][2] = b[3][4];
        b[2] = b[5];
        return b;
    }

    public static int[][] arReturn33() {
        int[][] i = new int[5][12];
        i[2] = new int[] {1, 2, 3};
        return i;
    }

    public static int[][][] arReturn34() {
        int[][][] i = new int[15][16][];
        i[2][3][4] = 2;
        i[5][6] = new int[] {7, 8, 9};
        i[10] = new int[][] {{11, 12}, {13, 14}, {}, null};
        i[11] = new int[][] {{}};
        return i;
    }

    public static ArrayReturn[][] arReturn35() {
        ArrayReturn[][] result = new ArrayReturn[5][12];
        result[4][3] = new ArrayReturn();
        result[3][1] = null;
        result[2] = new ArrayReturn[] {null, new ArrayReturn(), null};
        result[1] = null;
        return result;
    }

    public static ArrayReturn[][] arReturn36() {
        ArrayReturn[][] result = Mulib.namedFreeObject("name", ArrayReturn[][].class);
        ArrayReturn arrayReturn = new ArrayReturn();
        result[0][12] = new ArrayReturn();
        result[22][73] = null;
        result[-1][-1] = arrayReturn;
        return result;
    }

    public static ArrayReturn[][][] arReturn37() {
        ArrayReturn[][][] result = Mulib.freeObject(ArrayReturn[][][].class);
        result[0] = Mulib.freeObject(ArrayReturn[][].class);
        result[1] = Mulib.namedFreeObject("id0", ArrayReturn[][].class);
        result[2][3] = Mulib.namedFreeObject("id1", ArrayReturn[].class);
        result[4][5][6] = Mulib.freeObject(ArrayReturn.class);
        result[7][8][9] = Mulib.namedFreeObject("id2", ArrayReturn.class);
        return result;
    }

    public static int arReturn40() {
        ArrayReturn[] result = new ArrayReturn[0];
        return result.length;
    }

    public static int arReturn41() {
        ArrayReturn[][][] result = new ArrayReturn[0][][];
        return result.length;
    }

    public static int arReturn42() {
        int[] result = new int[0];
        return result.length;
    }
}
