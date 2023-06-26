package de.wwu.mulib.constraints;

import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.primitives.Sint;

public class PartnerClassObjectRememberConstraint implements PartnerClassObjectConstraint {

    private final String name;
    private final PartnerClass copied;

    public PartnerClassObjectRememberConstraint(String name, PartnerClass copied) {
        this.name = name;
        this.copied = copied;
    }

    public String getName() {
        return name;
    }

    public PartnerClass getRememberedValue() {
        return copied;
    }

    @Override
    public Sint getPartnerClassObjectId() {
        throw new MulibIllegalStateException("Should not be called"); //// TODO Refactor
    }
}
