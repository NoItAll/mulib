package de.wwu.mulib.transformer.examples;

import de.wwu.mulib.transformer.examples.class_hierarchies.C0;

import java.util.List;

public class HiddenIteratorVariable1 {

    public void iterate(double d, List<C0> list) {
        for (C0 element : list) {
            element.m0();
        }
    }
}
