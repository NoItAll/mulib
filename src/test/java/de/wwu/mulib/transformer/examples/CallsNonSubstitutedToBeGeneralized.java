package de.wwu.mulib.transformer.examples;

import de.wwu.mulib.transformer.examples.class_hierarchies.C0;

import java.util.List;

public class CallsNonSubstitutedToBeGeneralized {

    private int manualSize(List<C0> c0s) {
        int result = 0;
        for (C0 e : c0s) {
            result++;
        }
        return result;
    }
}
