package de.wwu.mulib.search.trees;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.solving.Labels;
import de.wwu.mulib.substitutions.primitives.Sbool;

public class Solution {

    public final Object value;
    public final Labels labels;
    public final Constraint[] additionalConstraints;

    public Solution(Object value, Labels labels, Constraint... additionalConstraints) {
        this.value = value;
        this.labels = labels;
        if (additionalConstraints.length == 0) {
            this.additionalConstraints = new Constraint[] { Sbool.ConcSbool.TRUE };
        } else {
            this.additionalConstraints = additionalConstraints;
        }
    }
}
