package de.wwu.mulib.constraints;

import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.primitives.Sint;

/**
 * Special "constraint" for remembering a snapshot of an array or a non-array object.
 * The snapshot is included in the set of labels given the specified name. Lazy initialization is taken into account,
 * i.e., if new values for the fields of a non-array object are lazily initialized, those are included in the snapshot.
 * Similarly, values lazily initialized by an array with undetermined length are also included in the snapshot.
 */
public class PartnerClassObjectRememberConstraint implements PartnerClassObjectConstraint {

    private final String name;
    private final PartnerClass copy;

    /**
     * Creates new remember constraint
     * @param name The name to remember the snapshot by
     * @param copy A copy. If the object is lazily initialized, this copy potentially is incomplete
     */
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
