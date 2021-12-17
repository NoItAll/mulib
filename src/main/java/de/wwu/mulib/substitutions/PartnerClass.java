package de.wwu.mulib.substitutions;

import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.transformations.MulibValueTransformer;

public interface PartnerClass extends SubstitutedVar {

    Object label(Object originalContainer, MulibValueTransformer mulibValueTransformer, SolverManager solverManager);

    Object copy(MulibValueTransformer mulibValueTransformer);

    Class<?> getOriginalClass();
}
