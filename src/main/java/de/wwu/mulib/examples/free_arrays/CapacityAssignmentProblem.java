package de.wwu.mulib.examples.free_arrays;

import de.wwu.mulib.Fail;
import de.wwu.mulib.Mulib;

public class CapacityAssignmentProblem {

    public static int[] assign(int[] machineCapacities, int[] workloads) {
        int[] assignment = new int[workloads.length];
        for (int i = 0; i < workloads.length; i++) {
            int assignedMachine = Mulib.freeInt();
            if (machineCapacities[assignedMachine] < workloads[i]) {
                throw Mulib.fail();
            }
            machineCapacities[assignedMachine] = machineCapacities[assignedMachine] - workloads[i];
            assignment[i] = assignedMachine;
        }
        return assignment;
    }

    private static int[] copy(int[] toCopy) {
        int[] copied = new int[toCopy.length];
        for (int i = 0; i < copied.length; i++) {
            copied[i] = toCopy[i];
        }
        return copied;
    }

    public static int[][][] assignWithPreproduction(int[] machineCapacitiesPerPeriod, int[][] workloadsWithDeadlineInPeriod) {
        final int numberPeriods = workloadsWithDeadlineInPeriod.length;
        int[][] overallCapacities = new int[numberPeriods][];
        // Fill up overallCapacities
        for (int i = 0; i < numberPeriods; i++) {
            overallCapacities[i] = copy(machineCapacitiesPerPeriod);
        }
        int[][][] assignmentsPerPeriod = new int[numberPeriods][][];
        for (int i = 0; i < numberPeriods; i++) {
            int[] currentSetOfWorkloads = workloadsWithDeadlineInPeriod[i];
            assignmentsPerPeriod[i] = new int[currentSetOfWorkloads.length][];
            for (int j = 0; j < currentSetOfWorkloads.length; j++) {
                int chosenMachineIndex = Mulib.freeInt();
                int chosenMachinePeriod = Mulib.freeInt();
                int[] chosenPeriodCapacities = overallCapacities[chosenMachinePeriod];
                int chosenCapacity = chosenPeriodCapacities[chosenMachineIndex];
                if (chosenMachinePeriod > i || chosenCapacity < currentSetOfWorkloads[j]) {
                    throw new Fail();
                }
                chosenPeriodCapacities[chosenMachineIndex] = chosenCapacity - currentSetOfWorkloads[j];
                assignmentsPerPeriod[i][j] = new int[] { chosenMachinePeriod, chosenMachineIndex };
            }
        }
        return assignmentsPerPeriod;
    }


}