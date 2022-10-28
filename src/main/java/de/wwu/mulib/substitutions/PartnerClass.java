package de.wwu.mulib.substitutions;

import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.transformations.MulibValueCopier;

import java.util.Map;

public interface PartnerClass extends IdentityHavingSubstitutedVar {

    Object label(Object originalContainer, SolverManager solverManager);

    Object copy(MulibValueCopier mulibValueTransformer);

    Class<?> getOriginalClass();

    default Map<String, SubstitutedVar> __mulib__getFieldNameToSubstitutedVar() {
        throw new MulibIllegalStateException("Should not occur");
    }

    default Map<String, Class<?>> __mulib__getFieldNameToType() {
        throw new MulibIllegalStateException("Should not occur");
    }
}
