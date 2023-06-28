package de.wwu.mulib.constraints;

import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.primitives.Sint;

public class PartnerClassObjectRememberConstraint implements PartnerClassObjectConstraint {

    private final String name;
    private final PartnerClass copy;

    public PartnerClassObjectRememberConstraint(String name, PartnerClass copy) {
        this.name = name;
        this.copy = copy;
    }

    public String getName() {
        return name;
    }

    public PartnerClass getRememberedValue() {
        return copy;
    }

    @Override
    public Sint getPartnerClassObjectId() {
        throw new MulibIllegalStateException("Should not be called"); // TODO Refactor
    }
}
