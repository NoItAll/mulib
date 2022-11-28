package de.wwu.mulib.transform_and_execute.examples.free_arrays;

import de.wwu.mulib.Fail;
import de.wwu.mulib.Mulib;

public class MachineCAP {

    public static class Machine {
        public int i;
        public Machine(int i) {
            this.i = i;
        }
    }

    // machineCapacities' type has been replaced since only in this array, free indices are used
    public static int[] assign(Machine[] machineCapacities, int[] workloads) {
        int[] assignment = new int[workloads.length];
        for (int i = 0; i < workloads.length; i++) {
            int assignedMachine = Mulib.freeInt();
            Machine vc = machineCapacities[assignedMachine];
            int val = vc.i;
            if (val < workloads[i]) {
                throw Mulib.fail();
            }
            vc = new Machine(val - workloads[i]);
            machineCapacities[assignedMachine] = vc;
            assignment[i] = assignedMachine;
        }
        return assignment;
    }

    // machineCapacities' type has been replaced since only in this array, free indices are used
    public static int[] assignMutateFieldValue(Machine[] machineCapacities, int[] workloads) {
        int[] assignment = new int[workloads.length];
        for (int i = 0; i < workloads.length; i++) {
            int assignedMachine = Mulib.freeInt();
            Machine vc = machineCapacities[assignedMachine];
            int val = vc.i;
            if (val < workloads[i]) {
                throw Mulib.fail();
            }
            vc.i -= workloads[i];
            assignment[i] = assignedMachine;
        }
        return assignment;
    }

    public static Machine[] copy(Machine[] toCopy) {
        Machine[] copied = new Machine[toCopy.length];
        for (int i = 0; i < copied.length; i++) {
            copied[i] = new Machine(toCopy[i].i);
        }
        return copied;
    }

    // machineCapacitiesPerPeriod' type has been replaced since only in this array, free indices are used
    public static int[][][] assignWithPreproduction(Machine[] machineCapacitiesPerPeriod, int[][] workloadsWithDeadlineInPeriod) {
        final int numberPeriods = workloadsWithDeadlineInPeriod.length;
        Machine[][] overallCapacities = new Machine[numberPeriods][];
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
                Machine[] valueContainersOfPeriod = overallCapacities[chosenMachinePeriod];
                Machine vc = valueContainersOfPeriod[chosenMachineIndex];
                int chosenCapacity = vc.i;
                if (chosenMachinePeriod > i || chosenCapacity < currentSetOfWorkloads[j]) {
                    throw new Fail();
                }
                int newCapacity = chosenCapacity - currentSetOfWorkloads[j];
                valueContainersOfPeriod[chosenMachineIndex] = new Machine(newCapacity);
                assignmentsPerPeriod[i][j] = new int[] { chosenMachinePeriod, chosenMachineIndex };
            }
        }
        return assignmentsPerPeriod;
    }


    // machineCapacitiesPerPeriod' type has been replaced since only in this array, free indices are used
    public static int[][][] assignWithPreproductionMutateFieldValue(Machine[] machineCapacitiesPerPeriod, int[][] workloadsWithDeadlineInPeriod) {
        final int numberPeriods = workloadsWithDeadlineInPeriod.length;
        Machine[][] overallCapacities = new Machine[numberPeriods][];
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
                Machine[] valueContainersOfPeriod = overallCapacities[chosenMachinePeriod];
                Machine vc = valueContainersOfPeriod[chosenMachineIndex];
                int chosenCapacity = vc.i;
                if (chosenMachinePeriod > i || chosenCapacity < currentSetOfWorkloads[j]) {
                    throw new Fail();
                }
                int newCapacity = chosenCapacity - currentSetOfWorkloads[j];
                vc.i = newCapacity;
                assignmentsPerPeriod[i][j] = new int[] { chosenMachinePeriod, chosenMachineIndex };
            }
        }
        return assignmentsPerPeriod;
    }


}