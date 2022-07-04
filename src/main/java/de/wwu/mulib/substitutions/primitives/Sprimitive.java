package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.substitutions.SubstitutedVar;

public interface Sprimitive extends SubstitutedVar {

    default String additionToToStringBody() {
        return "";
    }

}
