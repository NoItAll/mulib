package de.wwu.mulib.solving;

/**
 * The object encapsulating a solution to the search region.
 * Remembered variables end up in {@link Labels} where they can be retrieved for their names.
 */
public class Solution {
    /**
     * The return value
     */
    public final Object returnValue;
    /**
     * The labels. This also contains the return value
     */
    public final Labels labels;

    /**
     * Constructs a new solution
     * @param returnValue The return value
     * @param labels The labels, including the return value
     */
    public Solution(Object returnValue, Labels labels) {
        this.returnValue = returnValue;
        this.labels = labels;
    }

    @Override
    public String toString() {
        return "Solution{labels=" + labels + ", returnValue=" + returnValue + "}";
    }
}
