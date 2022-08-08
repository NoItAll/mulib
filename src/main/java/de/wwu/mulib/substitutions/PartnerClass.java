package de.wwu.mulib.substitutions;

import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.transformations.MulibValueCopier;

public interface PartnerClass extends SubstitutedVar {

    Object label(Object originalContainer, SolverManager solverManager);

    Object copy(MulibValueCopier mulibValueTransformer);

    Class<?> getOriginalClass();
}
