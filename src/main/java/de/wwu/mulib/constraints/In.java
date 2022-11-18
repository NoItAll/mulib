package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class In implements Constraint {

    private final Sprimitive element;
    private final Sprimitive[] set;

    protected In(Sprimitive element, Sprimitive[] set) {
        this.element = element;
        this.set = set;
    }

    public static Constraint newInstance(Sprimitive element, Collection<? extends Sprimitive> set) {
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
        return new In(element, toCheckElements.toArray(Sprimitive[]::new));
    }

    public Sprimitive getElement() {
        return element;
    }

    public Sprimitive[] getSet() {
        return set;
    }

    public boolean isBool() {
        return element instanceof Sbool;
    }

    public boolean isNumber() {
        return element instanceof Snumber;
    }

    public boolean isFp() {
        return element instanceof Sdouble || element instanceof Sfloat;
    }
}
