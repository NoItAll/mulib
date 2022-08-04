package de.wwu.mulib.search.trees;

import de.wwu.mulib.solving.Labels;

public class Solution {
    public final Object returnValue;
    public final Labels labels;

    public Solution(Object returnValue, Labels labels) {
        this.returnValue = returnValue;
        this.labels = labels;
    }

    public String toString() {
        return "Solution{labels=" + labels + ", returnValue=" + returnValue + "}";
    }
}
