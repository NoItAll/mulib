package de.wwu.mulib.examples;

import de.wwu.mulib.Mulib;

public class ThreePartition {
    public static int[][] execute(int[] vals) {
        int numberPartitions = vals.length / 3;
        Mulib.assume(vals.length == numberPartitions * 3);
        int[][] result = new int[numberPartitions][3];
        boolean[] alreadyPicked = new boolean[vals.length];
        int sameSum = Mulib.freeInt();
        for (int i = 0; i < numberPartitions; i++) {
            int[] valuesForPartition = result[i];
            for (int j = 0; j < 3; j++) {
                int pick = Mulib.freeInt();
                Mulib.assume(!alreadyPicked[pick]);
                valuesForPartition[j] = vals[pick];
                alreadyPicked[pick] = true;
            }
            Mulib.assume(sameSum == valuesForPartition[0] + valuesForPartition[1] + valuesForPartition[2]);
        }
        return result;
    }
}
