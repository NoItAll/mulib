package de.wwu.mulib.transformer.examples;

public class ControlFlowStatements {

    public static void multipleVarsForSameIndex0(boolean b) {
        if (b) {
            int var = 0;
            System.out.println(var);
        } else {
            boolean var = true;
            System.out.println(var);
        }
    }

    public static void multipleVarsForSameIndex1(boolean b) {
        if (b) {
            double var = 0;
            System.out.println(var);
        } else {
            boolean var = true;
            System.out.println(var);
        }
    }


    // Check for multiple return branches.
    public static int ifStatement0(int i) {
        if (i > 0) {
            return i;
        } else {
            return -i;
        }
    }

    // Check for multiple if-else-statements with multiple return branches.
    public static int ifStatement1(int i) {
        if (i > 0) {
            return i;
        } else if (i > -4) {
            i = i + 4;
        } else if (i > -8) {
            return i + 8;
        } else {
            i = -i;
        }
        return i;
    }

    // Check for multiple if-else-statements with one return branch.
    public static int ifStatement2(int i) {
        if (i > 0) {
            i = i;
        } else if (i > -4) {
            i = i + 4;
        } else if (i > -8) {
            i += 8;
        } else {
            i -= i;
        }
        return i;
    }

    // Check for instructions which only are tainted in some branches.
    public static int ifStatement3(int i0, int i1) {
        int i0New; // Is always tainted
        int i2 = 0; // Is always tainted
        int i3 = 1; // Is tainted in some branches
        int i4 = (int) (double) (int) (double) 3.1; // Is never tainted, but used to compute tainted values with.
        if (i0 > 0) {
            i0New = i0;
            i2 = i0New + i0;
        } else if (i0 > -4) {
            i0New = i0 + 4;
        } else {
            i0New = i0;
        }

        if (i1 > 0) {
            return i1;
        } else {
            i3 = i2 + i4 + i1;
            i1 = -i1;
        }

        if (i0 > -8) {
            return i0 + 8 + i3;
        } else if (i0 > -12) {
            i0 = -i0;
        } else {
            return i0New;
        }

        if (i1 > 3) {
            return i0 + i1;
        }

        return i4;
    }

    public static int ifStatement4(int i0, int i1) {
        boolean b = i0 < i1;
        b = b && i0 > 1;
        if (b) {
            i0++;
        }
        return i0;
    }

    public static int ifStatement5(int i0, int i1) {
        boolean b = i0 < i1 || i0 < 0;
        if (b) {
            return i0;
        }

        b = i0 > i1 || i0 <= 25;
        if (b) {
            return i1;
        } else {
            return i0;
        }
    }

    public static int ifStatement6(int i0, int i1) {
        if (i0 < i1 && i0 > 1) {
            i0++;
        }

        if (i0 < i1 || i0 < 0) {
            return i0;
        }

        if (i0 > i1 || i0 <= 25) {
            return i1;
        } else {
            return i0;
        }
    }

    public static int loop0(int toAddTo) {
        int result = toAddTo;
        for (int i = 0; i < 50; i++) {
            result += 12;
        }
        return result;
    }

    public static int loop1(int toAddTo, int numberIterations) {
        int result = toAddTo;
        for (int i = 0; i < numberIterations; i++) {
            result += 12;
        }
        return result;
    }

    public static int loop2(int toAddTo, int numberIterations) {
        int result = 0;
        while (numberIterations > 0) {
            numberIterations--;
            result += toAddTo;
        }
        result = 2;
        return result;
    }

    public static int loop3(int numberIterations) {
        int result = 0;
        while (numberIterations > 0) {
            numberIterations--;
            result = 4 + result;
            numberIterations--;
        }
        result += 2;
        return result;
    }


    // Check for simple recursion.
    public static int recursive0(int i) {
        if (i < 0) {
            return 1;
        }
        return i * recursive0(i - 1);
    }

    // Check two foreign recursion calls.
    public static int recursive1(int i) {
        return recursive0(i) * recursive0(i-2);
    }

    // Check foreign and own recursion call.
    public static int recursive2(int i) {
        if (i < 0) {
            return recursive0(i);
        }
        int x = recursive2(i-1);
        int y = recursive2(i-2);
        return x + y;
    }

    // Check recursive call with parameter swap.
    public static int recursive3(int i0, int i1, int i2, int cond) {
        i2 -= i0;
        i2 += i1;

        if (cond == 0) {
            i2 = recursive3(i1, i0, i2, --cond);
        } else {
        }

        return i2;
    }
}
