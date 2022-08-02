package de.wwu.mulib.substitutions;

import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.transformations.MulibValueCopier;
import de.wwu.mulib.transformations.MulibValueLabeler;

public interface PartnerClass extends SubstitutedVar {

    Object label(Object originalContainer, MulibValueLabeler mulibValueTransformer, SolverManager solverManager);

    Object copy(MulibValueCopier mulibValueTransformer);

    Class<?> getOriginalClass();
}
