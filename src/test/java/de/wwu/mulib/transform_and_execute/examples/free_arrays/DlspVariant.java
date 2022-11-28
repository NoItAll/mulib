package de.wwu.mulib.transform_and_execute.examples.free_arrays;

import de.wwu.mulib.Mulib;

public class DlspVariant {

    public static class Machine {
        public final int number;
        public final int productionCapacity;
        public final int[] producibleProductTypes;

        public Machine(
                int number,
                int productionCapacity,
                int[] producibleProductTypes) {
            this.number = number;
            this.productionCapacity = productionCapacity;
            this.producibleProductTypes = producibleProductTypes;
        }
    }

    public static class Product {
        public final int type;
        public final int requiredCapacity;
        public Machine producedBy;
        public Product(int type, int requiredCapacity) {
            this.type = type;
            this.requiredCapacity = requiredCapacity;
        }
    }

    public static Product[][] assign(Machine[] input, Product[][] productsInPeriod) {
        boolean[][] machinesUsedForPeriods = new boolean[productsInPeriod.length][input.length];
        int numPeriods = productsInPeriod.length;
        for (int i = 0; i < numPeriods; i++) {
            int numProductsInPeriod = productsInPeriod[i].length;
            for (int j = 0; j < numProductsInPeriod; j++) {
                Product currentProduct = productsInPeriod[i][j];
                int chosenMachineIndex = Mulib.freeInt();
                Machine chosenMachine = input[chosenMachineIndex];
                int chosenTypeIndex = Mulib.freeInt();
                int chosenType = chosenMachine.producibleProductTypes[chosenTypeIndex];
                boolean[] machinesUsedInPeriod = machinesUsedForPeriods[i];
                if (chosenType != currentProduct.type
                        || chosenMachine.productionCapacity < currentProduct.requiredCapacity
                        || machinesUsedInPeriod[chosenMachineIndex]) {
                    throw Mulib.fail();
                }
                machinesUsedInPeriod[chosenMachineIndex] = true;
                currentProduct.producedBy = chosenMachine;
            }
        }

        return productsInPeriod;
    }
}
