package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sint;

public interface ArrayConstraint extends IdentityHavingSubstitutedVarConstraint {

    Sint getArrayId();

}
