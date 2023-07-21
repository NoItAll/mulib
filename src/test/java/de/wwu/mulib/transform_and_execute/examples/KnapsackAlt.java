package de.wwu.mulib.transform_and_execute.examples;

import java.util.ArrayList;

import static de.wwu.mulib.Mulib.assume;
import static de.wwu.mulib.Mulib.freeInt;

public class KnapsackAlt {
    Item[] items = {new Item(750,10),
            new Item(1000,20),
            new Item(300,10),
            new Item(250,20),
            new Item(500,5),
            new Item(800,50),
            new Item(100,3)};


    public static class Item{
        public int weight;
        public int benefit;
        public boolean alreadyConsidered = false;

        Item(int w, int b){
            weight = w; benefit = b;
        }
    }

    ArrayList<Item> fillKnapsack(int capacity) {
        ArrayList<Item> content = new ArrayList<>();
        int weight = 0;
        for (int i = 0; i < items.length; i++) {
            int index = freeInt();
            Item item = items[index];
            assume(!item.alreadyConsidered);
            if (weight + item.weight <= capacity) {
                content.add(item);
                weight += item.weight;
            }
            // Don't evaluate twice
            item.alreadyConsidered = true;
        }
        assume(weight <= capacity);
        return content;
    }

    public static ArrayList<Item> findKnapsack() {
        KnapsackAlt knapsack = new KnapsackAlt();
        ArrayList<Item> content = knapsack.fillKnapsack(3250); // capacity 3250 g
        return content;
    }
}