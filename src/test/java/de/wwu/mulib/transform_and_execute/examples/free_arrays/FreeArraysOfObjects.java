package de.wwu.mulib.transform_and_execute.examples.free_arrays;

import de.wwu.mulib.Mulib;

public class FreeArraysOfObjects {

    public static class Truck {
        int loadedWeight = 0;
        final int capacity;
        Material[] loadedMaterials = new Material[0];

        public Truck(int capacity) {
            this.capacity = capacity;
        }

        boolean canLoad(Material m) {
            return capacity < loadedWeight + m.weight;
        }

        void load(Material m) {
            if (m.weight + loadedWeight > capacity) {
                throw new IllegalStateException("Must not occur");
            }
            loadedWeight += m.weight;
            Material[] currentMaterials = loadedMaterials;
            loadedMaterials = new Material[currentMaterials.length + 1];
            // Copy
            for (int i = 0; i < currentMaterials.length; i++) {
                loadedMaterials[i] = currentMaterials[i];
            }
            loadedMaterials[loadedMaterials.length - 1] = m;
        }
    }

    public static class Material {
        final String name;
        final int weight;

        public Material(String name, int weight) {
            this.name = name;
            this.weight = weight;
        }
    }

    public static Truck[] assignMaterials(Truck[] trucks, Material[] materials) {
        for (int i = 0; i < materials.length; i++) {
            int chosenTruckIndex = Mulib.freeInt();
            Truck chosenTruck = trucks[chosenTruckIndex];
            if (!chosenTruck.canLoad(materials[i])) {
                throw Mulib.fail();
            }
            chosenTruck.load(materials[i]);
        }
        return trucks;
    }
}
