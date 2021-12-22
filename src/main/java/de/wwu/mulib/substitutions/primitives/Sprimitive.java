package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.SubstitutedVar;

public interface Sprimitive extends SubstitutedVar {

    default String additionToToStringBody() {
        return "";
    }

    long getId();

    default String getInternalName() {
        return this.getClass().getSimpleName() + getId();
    }

}
