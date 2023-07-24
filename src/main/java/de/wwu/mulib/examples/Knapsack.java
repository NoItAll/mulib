package de.wwu.mulib.examples;

import de.wwu.mulib.Mulib;

import java.util.ArrayList;

import static de.wwu.mulib.Mulib.assume;

public class Knapsack {
    Item[] items = {new Item("bread",750,10),
            new Item("jam",500,5),
            new Item("water",1000,20),
            new Item("juice",750,15),
            new Item("socks",100,5),
            new Item("shirt",300,10),
            new Item("meat",250,20),
            new Item("knife",500,5),
            new Item("sleeping bag",800,50),
            new Item("lighter",100,3)};

    public static class Item {
        public String item;
        public int weight;
        public int benefit;

        Item(String it, int w, int b){
            item = it; weight = w; benefit = b;
        }
    }

    ArrayList<Item> fillKnapsack(int capacity){
        int weight = 0;
        ArrayList<Item> result = new ArrayList<>();
        for (Item item : items) {
            if (weight >= capacity) {
                break;
            }
            boolean take = Mulib.freeBoolean();
            if (weight + item.weight <= capacity && take) {
                result.add(item);
                weight += item.weight;
            }
        }
        assume(weight == capacity);
        return result;
    }

    public static ArrayList<Item> findKnapsack() {
        Knapsack knapsack = new Knapsack();
        return knapsack.fillKnapsack(4600); // capacity 4600 g
    }
}