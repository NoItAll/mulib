package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sint;

/**
 * Represents a constraint concerning a {@link de.wwu.mulib.substitutions.Sarray}- or
 * {@link de.wwu.mulib.substitutions.PartnerClass}-object.
 */
public interface PartnerClassObjectConstraint {

    /**
     * @return The identifier representing the object this constraint belongs to
     */
    Sint getPartnerClassObjectId();

}
