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
        Class<short[]> parameter = short[].class;
        intAr0 = Mulib.freeIntArray();
        doubleAr0 = Mulib.freeDoubleArray();
        floatAr0 = Mulib.freeFloatArray();
        longAr0 = Mulib.freeLongArray();
        shortArs0 = Mulib.freeArray(parameter);
        byteArs0 = Mulib.freeArray(byte[].class);
        booleanArs0 = Mulib.freeArray(boolean[][].class);
        objectArray0 = Mulib.freeArray(ArrayFieldsWithInitialization.class);
    }

    public ArrayFieldsWithInitialization(boolean b0) {
        Class<short[]> parameter = short[].class;
        intAr0 = Mulib.namedFreeIntArray("is0");
        doubleAr0 = Mulib.namedFreeDoubleArray("ds0");
        floatAr0 = Mulib.namedFreeFloatArray("fs0");
        longAr0 = Mulib.namedFreeLongArray("ls0");
        shortArs0 = Mulib.namedFreeArray("ss0", parameter);
        byteArs0 = Mulib.namedFreeArray("bs0", byte[].class);
        booleanArs0 = Mulib.namedFreeArray("bools0", boolean[][].class);
        objectArray0 = Mulib.namedFreeArray("os0", ArrayFieldsWithInitialization.class);
    }

}
