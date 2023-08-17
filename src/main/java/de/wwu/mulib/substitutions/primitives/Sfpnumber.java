package de.wwu.mulib.substitutions.primitives;

/**
 * Marker interface for floating-point numbers, i.e., {@link Sfloat} and {@link Sdouble}.
 */
public abstract class Sfpnumber extends AbstractSnumber {

    @Override
    public final boolean isFp() {
        return true;
    }

}
