package de.wwu.mulib.transformer.examples.free_arrays;

import de.wwu.mulib.Mulib;

public class ArrayFieldsWithInitialization {
    private int[] intAr0;
    protected double[] doubleAr0;
    public float[] floatAr0;
    long[] longAr0;
    short[][] shortArs0;
    private byte[][] byteArs0;
    private boolean[][][] booleanArs0;
    ArrayFieldsWithInitialization[] objectArray0;

    // Named
    private int[] intAr1;
    protected double[] doubleAr1;
    public float[] floatAr1;
    long[] longAr1;
    short[][] shortArs1;
    private byte[][] byteArs1;
    private boolean[][][] booleanArs1;
    ArrayFieldsWithInitialization[] objectArray1;

//    // Fixed size // TODO Enable
//    private int[] intAr2;
//    protected double[] doubleAr2;
//    public float[] floatAr2;
//    long[] longAr2;
//    short[][] shortArs2;
//    private byte[][] byteArs2;
//    private boolean[][][] booleanArs2;
//    ArrayFieldsWithInitialization[] objectArray2;

//
//    // Named and fixed size
//    private int[] intAr3;
//    protected double[] doubleAr3;
//    public float[] floatAr3;
//    long[] longAr3;
//    short[][] shortArs3;
//    private byte[][] byteArs3;
//    private boolean[][][] booleanArs3;
//    ArrayFieldsWithInitialization[] objectArray3;

    public ArrayFieldsWithInitialization() {
        Class<short[][]> parameter = short[][].class;
        intAr0 = Mulib.freeIntArray();
        doubleAr0 = Mulib.freeDoubleArray();
        floatAr0 = Mulib.freeFloatArray();
        longAr0 = Mulib.freeLongArray();
        shortArs0 = Mulib.freeObject(parameter);
        byteArs0 = Mulib.freeObject(byte[][].class);
        booleanArs0 = Mulib.freeObject(boolean[][][].class);
        objectArray0 = Mulib.freeObject(ArrayFieldsWithInitialization[].class);
    }

    public ArrayFieldsWithInitialization(boolean b0) {
        Class<short[][]> parameter = short[][].class;
        intAr0 = Mulib.namedFreeIntArray("is0");
        doubleAr0 = Mulib.namedFreeDoubleArray("ds0");
        floatAr0 = Mulib.namedFreeFloatArray("fs0");
        longAr0 = Mulib.namedFreeLongArray("ls0");
        shortArs0 = Mulib.namedFreeObject("ss0", parameter);
        byteArs0 = Mulib.namedFreeObject("bs0", byte[][].class);
        booleanArs0 = Mulib.namedFreeObject("bools0", boolean[][][].class);
        objectArray0 = Mulib.namedFreeObject("os0", ArrayFieldsWithInitialization[].class);
    }

    public ArrayFieldsWithInitialization(boolean b0, boolean b1) {
        Class<short[][]> parameter = short[][].class;
        intAr0 = new int[0];
        doubleAr0 = new double[0];
        floatAr0 = new float[0];
        longAr0 = new long[0];
        shortArs0 = new short[0][0];
        byteArs0 = new byte[0][0];
        booleanArs0 = new boolean[0][][];
        objectArray0 = new ArrayFieldsWithInitialization[0];
    }

}
