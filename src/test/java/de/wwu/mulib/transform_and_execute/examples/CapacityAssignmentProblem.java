package de.wwu.mulib.transform_and_execute.examples;

import de.wwu.mulib.Fail;
import de.wwu.mulib.Mulib;

public class CapacityAssignmentProblem {

    // To compare with performance of assign:
    public static int[] assignWithoutFreeIndex(int[] machineCapacities, int[] workloads) {
        int[] assignment = new int[workloads.length];
        for (int i = 0; i < workloads.length; i++) {
            for (int j = 0; j < machineCapacities.length; j++) {
                if (machineCapacities[j] >= workloads[i]) {
                    machineCapacities[j] = machineCapacities[j] - workloads[i];
                    assignment[i] = j;
                    break;
                }
            }
        }
        return assignment;
    }

    public static int[] assign(int[] machineCapacities, int[] workloads) {
        int[] assignment = new int[workloads.length];
        for (int i = 0; i < workloads.length; i++) {
            int assignedMachine = Mulib.namedFreeInt("workload_" + i);
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

    // Just here to be transformed:
    private static int[][] copy(int[][] toCopy) {
        int[][] copied = new int[toCopy.length][];
        for (int i = 0; i < toCopy.length; i++) {
            copied[i] = copy(toCopy[i]);
        }
        return copied;
    }
    public static int[][] assign(int[] machineCapacitiesPerPeriod, int[][] workloadsPerPeriod) {
        final int numberPeriods = workloadsPerPeriod.length;
        int[][] overallCapacities = new int[numberPeriods][];
        // Fill up overallCapacities
        for (int i = 0; i < numberPeriods; i++) {
            overallCapacities[i] = copy(machineCapacitiesPerPeriod);
        }
        int[][] assignmentsPerPeriod = new int[numberPeriods][];
        for (int i = 0; i < numberPeriods; i++) {
            assignmentsPerPeriod[i] = assign(overallCapacities[i], workloadsPerPeriod[i]);
        }
        return assignmentsPerPeriod;
    }

    public static int[][] assignWithPreproduction(int[] machineCapacitiesPerPeriod, int[][] workloadsWithDeadlineInPeriod) {
        final int numberPeriods = workloadsWithDeadlineInPeriod.length;
        int[][] overallCapacities = new int[numberPeriods][];
        // Fill up overallCapacities
        for (int i = 0; i < numberPeriods; i++) {
            overallCapacities[i] = copy(machineCapacitiesPerPeriod);
        }
        int[][] assignmentsPerPeriod = new int[numberPeriods][];
        for (int i = 0; i < numberPeriods; i++) {
            int[] currentSetOfWorkloads = workloadsWithDeadlineInPeriod[i];
            assignmentsPerPeriod[i] = new int[currentSetOfWorkloads.length];
            for (int j = 0; j < currentSetOfWorkloads.length; j++) {
                int chosenMachineIndex = Mulib.freeInt();
                int chosenMachinePeriod = Mulib.freeInt();
                int chosenCapacity = overallCapacities[chosenMachinePeriod][chosenMachineIndex];
                if (chosenMachinePeriod > i || chosenCapacity < currentSetOfWorkloads[j]) {
                    throw new Fail();
                }
                overallCapacities[chosenMachinePeriod][chosenMachineIndex] = chosenCapacity - currentSetOfWorkloads[j];
                assignmentsPerPeriod[i][j] = chosenMachineIndex;
            }
        }
        return assignmentsPerPeriod;
    }


}
