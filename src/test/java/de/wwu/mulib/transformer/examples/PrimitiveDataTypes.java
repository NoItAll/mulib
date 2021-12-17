package de.wwu.mulib.transformer.examples;

public class PrimitiveDataTypes {

    // Check all choice point commands for ints.
    public static int intIfCheck(int i0, int i1, int i2, int i3) {
        if (i0 == 0) {
            return i0;
        }
        if (i0 > 0) {
           return i1;
        }
        if (i1 < 0) {
            return i1;
        }
        if (i2 >= 0) {
            return i2;
        }
        if (i3 <= 0) {
            return i3;
        }
        if (i0 == i2) {
            return i0;
        }
        if (i0 > i2) {
            return i2;
        }
        if (i1 < i3) {
            return i1;
        }
        if (i0 >= i3 + 5) {
            return i3;
        }
        if (i3 <= i0 + 13) {
            return i3;
        }
        return 0;
    }

    // Check all choice point commands for doubles.
    public static double doubleIfCheck(double i0, double i1, double i2, double i3) {
        boolean b = i0 == 0.0;
        if (b) {
            return i0;
        }
        if (i0 > 0d) {
            return i1;
        }
        if (i1 < 0f) {
            return i1;
        }
        if (i2 >= 0) {
            return i2;
        }
        if (i3 <= 0L) {
            return i3;
        }
        if (i0 == i2) {
            return i0;
        }
        if (i0 > i2) {
            return i2;
        }
        if (i1 < i3) {
            return i1;
        }
        if (i0 >= i3 + 1.4) {
            return i3;
        }
        if (i3 <= i0 + 33.2) {
            return i3;
        }
        return 0;
    }

    // Check all choice point commands for floats.
    public static float floatIfCheck(float i0, float i1, float i2, float i3) {
        if (i0 == 0.0) {
            return i0;
        }
        if (i0 > 0d) {
            return i1;
        }
        if (i1 < 0f) {
            return i1;
        }
        if (i2 >= 0) {
            return i2;
        }
        if (i3 <= 0L) {
            return i3;
        }
        if (i0 == i2) {
            return i0;
        }
        if (i0 > i2) {
            return i2;
        }
        if (i1 < i3) {
            return i1;
        }
        if (i0 >= i3 + 1.4) {
            return i3;
        }
        if (i3 <= i0 + 33.2) {
            return i3;
        }
        return 0f;
    }

    // Check all choice point commands for doubles.
    public static boolean booleanIfCheck(boolean b0, boolean b1, boolean b3) {
        int count = 0;
        if (b0) {
            count += 1;
        }
        if (!b1) {
            count += 2;
        }
        if (!b0 && b1) {
            return b3;
        }

        if ((b3 && b1) || !b0) {
            return b3;
        }
        return b3 || (b0 && b1);
    }

    public static boolean booleanStack0(boolean b0, boolean b3) {
        boolean b1 = Boolean.parseBoolean("true");
        boolean b2 = Boolean.parseBoolean("false");
        return b0 && b1 || (b2 && b0) || b3;
    }

    public static boolean booleanStack1(boolean b0, boolean b3) {
        boolean b1 = Boolean.parseBoolean("true");
        boolean b2 = Boolean.parseBoolean("false");
        return b1 || (b2 && b0) || b3;
    }

    public static boolean booleanStack3(boolean b0, boolean b3) {
        boolean b1 = Boolean.parseBoolean("true");
        boolean b2 = Boolean.parseBoolean("false");
        return b1 && b0 || (b2 && b0);
    }

    public static boolean booleanStack4() {
        boolean b0 = Boolean.parseBoolean("true");
        boolean b1 = Boolean.parseBoolean("false");
        boolean b2 = Boolean.parseBoolean("true");
        boolean b3 = Boolean.parseBoolean("false");
        boolean b4 = Boolean.parseBoolean("true");
        boolean b5 = Boolean.parseBoolean("false");
        return b1 || (b2 && b0) || b3 || (b4 && b5);
    }

    public static short shortStack() {
        short s0 = 0;
        short s1 = 1;
        short s2 = 2;
        short s3 = -1;
        return (short) (s0 + s1 + s2 + s3);
    }

    public static short shortIfCheck(short s0, short s1, short s2) {
        if (s0 > s1) {
            return 0;
        } else if (s0 == s1) {
            return 1;
        } else if (s0 + 5 > s1) {
            return 2;
        } else if (s0 + 15 < s1) {
            return 3;
        } else if (s0 <= s1 - 35) {
            return 4;
        } else if (s0 * 32 >= s1 + 5) {
            return 5;
        } else if (s0 != s2) {
            return 6;
        }
        return 7;
    }

    public static short shortStores(short s0, short s1) {
        short s2 = 0;
        short s3 = 1;
        if (s0 > s1) {
            s3 = s2;
        } else {
            s3 = (short) (s0 + s1);
        }
        short s4 = 2;
        if (s3 > s2) {
            short s5 = 3;
            s2 += s5;
            return s2;
        } else {
            s3 += s4;
            return s3;
        }
    }

    public static byte byteStack() {
        byte s0 = 0;
        byte s1 = 1;
        byte s2 = 2;
        byte s3 = -1;
        return (byte) (s0 + s1 + s2 + s3);
    }

    public static byte byteIfCheck(byte s0, byte s1, byte s2) {
        if (s0 > s1) {
            return 0;
        } else if (s0 == s1) {
            return 1;
        } else if (s0 + 5 > s1) {
            return 2;
        } else if (s0 + 15 < s1) {
            return 3;
        } else if (s0 <= s1 - 35) {
            return 4;
        } else if (s0 * 32 >= s1 + 5) {
            return 5;
        } else if (s0 != s2) {
            return 6;
        }
        return 7;
    }

    public static byte byteStores(byte s0, byte s1) {
        byte s2 = 0;
        byte s3 = 1;
        if (s0 > s1) {
            s3 = s2;
        } else {
            s3 = (byte) (s0 + s1);
        }
        byte s4 = 2;
        if (s3 > s2) {
            byte s5 = 3;
            s2 += s5;
            return s2;
        } else {
            s3 += s4;
            return s3;
        }
    }

    public static long longStack() {
        long s0 = 0;
        long s1 = 1;
        long s2 = 2;
        long s3 = -1;
        return (s0 + s1 + s2 + s3);
    }

    public static long longIfCheck0(long s0, long s1) {
         if (s0 != s1) {
            return 1;
        }
        return 2;
    }

    public static long longIfCheck1(long s0, long s1, long s2) {
        if (s0 > s1) {
            return 0;
        } else if (s0 == s1) {
            return 1;
        } else if (s0 + 5 > s1) {
            return 2;
        } else if (s0 + 15 < s1) {
            return 3;
        } else if (s0 <= s1 - 35) {
            return 4;
        } else if (s0 * 32 >= s1 + 5) {
            return 5;
        } else if (s0 != s2) {
            return 6;
        }
        return 7;
    }

    public static long longStores0(long s0, long s1) {
        long s2 = 0;
        long s3 = (s0 + s1);
        if (s3 > s2) {
            return s2;
        }
        return s3;
    }

    public static long longStores1(long s0, long s1) {
        long s2 = 0;
        long s3 = 1;
        if (s0 > s1) {
            s3 = s2;
        } else {
            s3 = (s0 + s1);
        }
        long s4 = 2;
        if (s3 > s2) {
            long s5 = 3;
            s2 += s5;
            return s2;
        } else {
            s3 += s4;
        }
        return s3;
    }

}
