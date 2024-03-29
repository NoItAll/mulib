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

    // Fixed size
    private int[] intAr2;
    protected double[] doubleAr2;
    public float[] floatAr2;
    long[] longAr2;
    short[][] shortArs2;
    private byte[][] byteArs2;
    private boolean[][][] booleanArs2;
    ArrayFieldsWithInitialization[] objectArray2;


    // Named and fixed size /// TODO Functionality for named arrays of fixed size
    private int[] intAr3;
    protected double[] doubleAr3;
    public float[] floatAr3;
    long[] longAr3;
    short[][] shortArs3;
    private byte[][] byteArs3;
    private boolean[][][] booleanArs3;
    ArrayFieldsWithInitialization[] objectArray3;

    public ArrayFieldsWithInitialization() {
        Class<short[][]> parameter = short[][].class;
        intAr0 = Mulib.freeObject(int[].class);
        doubleAr0 = Mulib.freeObject(double[].class);
        floatAr0 = Mulib.freeObject(float[].class);
        longAr0 = Mulib.freeObject(long[].class);
        shortArs0 = Mulib.freeObject(parameter);
        byteArs0 = Mulib.freeObject(byte[][].class);
        booleanArs0 = Mulib.freeObject(boolean[][][].class);
        objectArray0 = Mulib.freeObject(ArrayFieldsWithInitialization[].class);
    }

    public ArrayFieldsWithInitialization(boolean b0) {
        Class<short[][]> parameter = short[][].class;
        intAr1 = Mulib.rememberedFreeObject("is1", int[].class);
        doubleAr1 = Mulib.rememberedFreeObject("ds1", double[].class);
        floatAr1 = Mulib.rememberedFreeObject("fs1", float[].class);
        longAr1 = Mulib.rememberedFreeObject("ls1", long[].class);
        shortArs1 = Mulib.rememberedFreeObject("ss1", parameter);
        byteArs1 = Mulib.rememberedFreeObject("bs1", byte[][].class);
        booleanArs1 = Mulib.rememberedFreeObject("bools1", boolean[][][].class);
        objectArray1 = Mulib.rememberedFreeObject("os1", ArrayFieldsWithInitialization[].class);
    }

    public ArrayFieldsWithInitialization(boolean b0, boolean b1) {
        Class<short[][]> parameter = short[][].class;
        intAr2 = new int[9];
        doubleAr2 = new double[13];
        floatAr2 = new float[0];
        longAr2 = new long[2];
        shortArs2 = new short[-1][];
        byteArs2 = new byte[0][0];
        booleanArs2 = new boolean[0][][];
        objectArray2 = new ArrayFieldsWithInitialization[5];
    }

}
