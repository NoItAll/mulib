package de.wwu.mulib.transform_and_execute.examples;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.transform_and_execute.examples.apache2_examples.WBSTransf;

// HW: Adapted to fit Mulib, taken from https://github.com/sosy-lab/sv-benchmarks/blob/99d37c5b4072891803b9e5c154127c912477f705/java/java-ranger-regression/WBS/prop1/Main.java
public class WBSProp1 {

    public static void exec() { // HW: Changed from main to exec
        WBSTransf wbs = new WBSTransf();
        int PedalPos;
        boolean AutoBrake, Skid;
        for (int i = 0; i < 2; i++) {
            PedalPos = Mulib.freeInt();
            AutoBrake = Mulib.freeBoolean();
            Skid = Mulib.freeBoolean();
            wbs.update(PedalPos, AutoBrake, Skid);
            // This assertion should fail:
            assert ((PedalPos > 0 && PedalPos <= 4 && !Skid) ? (wbs.Alt_Pressure > 0) : true);
        }
    }
}