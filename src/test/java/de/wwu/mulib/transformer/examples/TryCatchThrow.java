package de.wwu.mulib.transformer.examples;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.transformer.CustomException1;

public class TryCatchThrow {

    public void check0(int i) {
        try {
            int j = i/0;
            System.out.println(j);
        } catch (ArithmeticException e) {
            e.printStackTrace();
        }
    }

    public void check1(int i) throws Exception, CustomException0, CustomException1 {
        if (i == 0) {
            throw new CustomException0();
        }
        if (i == 1) {
            throw new Exception();
        }
        if (i == 2) {
            throw new CustomException1();
        }
    }

    public void check2(int i) {
        if (i == 0) {
            throw new CustomRuntimeException();
        }
    }

    public CustomRuntimeException check3(int i) {
        try {
            check1(i);
        } catch (Exception e) {
            // SymbolicExecution should also be accessible in catch-block!
            Mulib.freeInt();
        }
        return null;
    }

    public CustomRuntimeException check4(int i) {
        try {
            check1(i);
        } catch (CustomException0 e) {
            e.printStackTrace();
        } catch (CustomException1 e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        try {
            check4(i);
        } catch (CustomRuntimeException e) {
            e.printStackTrace();
            return e;
        }
        return new CustomRuntimeException();
    }

    public void check5(int i) throws CustomRuntimeException {
        if (i == 0 || i == 4 || ((i % 2) == 0 && i-1 == 8)) {
            throw new CustomRuntimeException();
        }
    }
}
