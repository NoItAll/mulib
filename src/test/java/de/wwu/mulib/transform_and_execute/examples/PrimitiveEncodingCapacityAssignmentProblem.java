package de.wwu.mulib.transform_and_execute.examples;

import de.wwu.mulib.Mulib;

public class PrimitiveEncodingCapacityAssignmentProblem {

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


}
