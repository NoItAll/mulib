package de.wwu.mulib.transform_and_execute.examples.generated_methods_check;

public class HasClinitInitialized {
    private static int INT_CONST = 0;
    private static long LONG_CONST = 1;
    private static double DOUBLE_CONST = 2;
    private static float FLOAT_CONST = 3;
    private static short SHORT_CONST = 4;
    private static byte BYTE_CONST = 5;
    private static boolean BOOL_CONST = false;

    public static int checkINT_CONST(int v) {
        if (v < INT_CONST) {
            return 1;
        } else {
            return 0;
        }
    }

    public static int checkLONG_CONST(long v) {
        if (v >= LONG_CONST) {
            if (v != LONG_CONST + 1) {
                return 1;
            } else {
                return 2;
            }
        } else {
            return 0;
        }
    }

    public static int checkDOUBLE_CONST(double v) {
        if (v <= DOUBLE_CONST) {
            return 1;
        } else {
            if (v == DOUBLE_CONST + 100) {
                return 2;
            } else {
                return 0;
            }
        }
    }

    public static int checkFLOAT_CONST(float v) {
        if (v < FLOAT_CONST) {
            return 1;
        } else {
            if (v == FLOAT_CONST) {
                return 0;
            } else {
                return 2;
            }
        }
    }

    public static int checkSHORT_CONST(short v) {
        if (v < SHORT_CONST) {
            return 1;
        } else {
            return 0;
        }
    }

    public static int checkBYTE_CONST(byte v) {
        if (v < BYTE_CONST) {
            return 1;
        } else {
            return 0;
        }
    }

    public static int checkBOOL_CONST(boolean v) {
        if (v == BOOL_CONST) {
            return 1;
        } else {
            return 0;
        }
    }
}
