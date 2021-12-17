package de.wwu.mulib.transform_and_execute.examples.generated_methods_check;

public class HasClinitUninitialized {
    private static int IGNORED = 5;
    private static int INT_CONST;
    private static long LONG_CONST;
    private static double DOUBLE_CONST;
    private static float FLOAT_CONST;
    private static short SHORT_CONST;
    private static byte BYTE_CONST;
    private static boolean BOOL_CONST;

    public static int checkINT_CONST(int v) {
        if (v > INT_CONST) {
            return 1;
        } else {
            return 0;
        }
    }

    public static int checkLONG_CONST(long v) {
        if (v > LONG_CONST) {
            return 1;
        } else {
            return 0;
        }
    }

    public static int checkDOUBLE_CONST(double v) {
        if (v > DOUBLE_CONST) {
            return 1;
        } else {
            return 0;
        }
    }

    public static int checkFLOAT_CONST(float v) {
        if (v > FLOAT_CONST) {
            return 1;
        } else {
            return 0;
        }
    }

    public static int checkSHORT_CONST(short v) {
        if (v > SHORT_CONST) {
            return 1;
        } else {
            return 0;
        }
    }

    public static int checkBYTE_CONST(byte v) {
        if (v > BYTE_CONST) {
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
