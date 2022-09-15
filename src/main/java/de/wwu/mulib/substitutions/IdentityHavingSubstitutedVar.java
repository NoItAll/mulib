package de.wwu.mulib.substitutions;

import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;

public interface IdentityHavingSubstitutedVar extends SubstitutedVar {

    Sint getId();

    void prepareToRepresentSymbolically(SymbolicExecution se);

    default Sbool isNull() {
        throw new MulibIllegalStateException("Must not occur"); //// TODO implement transformation
    }

    default void setIsNotNull() {
        throw new MulibIllegalStateException("Must not occur"); //// TODO implement transformation
    }

}
