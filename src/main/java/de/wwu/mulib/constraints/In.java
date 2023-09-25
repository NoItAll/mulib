package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a constraint asserting that an element is in a set of values
 */
public class In implements Constraint {

    private final Snumber element;
    private final Snumber[] set;

    protected In(Snumber element, Snumber[] set) {
        this.element = element;
        this.set = set;
    }

    /**
     * Creates a new constraint, possibly simplifying the overall constraint
     * @param element The element for which it is checked whether the
     * @param set The set of values that should contain element
     * @return A constraint that is either simplified or an "element of"-constraint
     */
    public static Constraint newInstance(Snumber element, Collection<? extends Snumber> set) {
        List<Sprimitive> toCheckElements = new ArrayList<>();
        boolean elementIsConc = element instanceof ConcSprimitive;
        for (Sprimitive setElement : set) {
            if (element.equals(setElement)) {
                return Sbool.ConcSbool.TRUE;
            }
            if (elementIsConc && setElement instanceof ConcSprimitive) {
                continue;
            }
            toCheckElements.add(setElement);
        }

        if (toCheckElements.isEmpty()) {
            return Sbool.ConcSbool.FALSE;
        }
        return new In(element, toCheckElements.toArray(Snumber[]::new));
    }

    /**
     * @return The element that is checked to belong to a set. Can also be a {@link Sbool}.
     */
    public Snumber getElement() {
        return element;
    }

    /**
     * @return The set for which it is checked whether an element comes from it. The elements of the set can also be
     * {@link Sbool}s.
     */
    public Snumber[] getSet() {
        return set;
    }

    /**
     * @return Whether we check for a {@link Sbool}
     */
    public boolean isBool() {
        return element instanceof Sbool;
    }

    /**
     * @return Whether we check for a {@link Sfpnumber}
     */
    public boolean isFp() {
        return element instanceof Sfpnumber;
    }
}
