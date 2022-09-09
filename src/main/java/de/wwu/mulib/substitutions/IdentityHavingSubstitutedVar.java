package de.wwu.mulib.substitutions;

import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.Sint;

public interface IdentityHavingSubstitutedVar extends SubstitutedVar {

    Sint getId();

    void prepareToRepresentSymbolically(SymbolicExecution se);

}
