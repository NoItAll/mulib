package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.Sarray;

public class ArrayRememberConstraint extends PartnerClassObjectRememberConstraint implements ArrayConstraint {
    public ArrayRememberConstraint(String name, Sarray<?> concreteCopy) {
        super(name, concreteCopy);
    }
}
