package de.wwu.mulib.substitutions;

import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;

public interface IdentityHavingSubstitutedVar extends SubstitutedVar {

    default Sint getId() {
        throw new MulibIllegalStateException("Must not occur"); //// TODO implement transformation
    }

    default void prepareToRepresentSymbolically(SymbolicExecution se) {
        throw new MulibIllegalStateException("Must not occur"); //// TODO implement transformation
    }

    default Sbool isNull() {
        throw new MulibIllegalStateException("Must not occur"); //// TODO implement transformation
    }

    default void setIsNotNull() {
        throw new MulibIllegalStateException("Must not occur"); //// TODO implement transformation
    }

    default boolean isRepresentedInSolver() {
        throw new MulibIllegalStateException("Must not occur"); //// TODO implement transformation
    }

    default boolean shouldBeRepresentedInSolver() {
        throw new MulibIllegalStateException("Must not occur"); //// TODO implement transformation
    }

}
