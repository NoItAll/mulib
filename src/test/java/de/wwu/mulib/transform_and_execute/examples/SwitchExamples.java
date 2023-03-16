package de.wwu.mulib.transform_and_execute.examples;

import de.wwu.mulib.Mulib;

public class SwitchExamples {

    public static int intSwitch() {
        int i = Mulib.namedFreeByte("i");

        switch (i) {
            case 0:
            case 1:
                i = 5;
            case 12:
                i += 5;
                break;
            default:
                i= -i;
        }

        return i;
    }


    public static int stringSwitch(String s) {
        int i = 0;
        switch (s) {
            case "0":
            case "1":
                i = 5;
            case "12":
                i += 5;
                break;
            default:
                i= -i;
        }

        return i;
    }

    public static int intSwitch2() {
        int i = Mulib.freeInt();
        switch (i) {
            case 12:
                i += 5;
                break;
            case 0:
            case 1:
                i = 5;
            default:
                i= -i;
        }

        return i;
    }


    public static int stringSwitch2(String s) {
        int i = 0;
        switch (s) {
            case "12":
                i += 5;
                break;
            case "0":
            case "1":
                i = 5;
            default:
                i= -i;
        }

        return i;
    }

    public static int intSwitch3() {
        int i = Mulib.namedFreeInt("i");

        switch (i) {
            case 0:
                i = 1;
                break;
            case 1:
                i = 2;
                break;
            case 2:
                i = 3;
                break;
            case 3:
                i = 4;
                break;
            case 4:
                i = 5;
                break;
            case 5:
                i = 6;
                break;
            case 6:
                i = 7;
                break;
            case 7:
                i = 1;
                break;
            case 9:
                i = 3;
                break;
            case 10:
                i = 4;
                break;
            case 11:
                i = 5;
                break;
            case 12:
                i = 6;
                break;
            case 13:
                i = 7;
                break;
        }
        return i;
    }

//    public static int enumSwitch(EnumForSwitch e, int i) { // TODO
//        switch (e) {
//            case ZERO:
//            case ONE:
//                i = 5;
//            case TWELVE:
//                i += 5;
//                break;
//            default:
//                i= -i;
//        }
//
//        return i;
//    }
//
//    enum EnumForSwitch {
//        ZERO, ONE, TWELVE;
//    }
}
