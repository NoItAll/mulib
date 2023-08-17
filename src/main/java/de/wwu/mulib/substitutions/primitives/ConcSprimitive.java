package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.substitutions.Conc;

/**
 * Marker interface for all classes representing concrete primitive values.
 * Since in Java, even char, and boolean in the bytecode, can be used as an int, pretty much boils down to {@link ConcSnumber}.
 */
public interface ConcSprimitive extends Sprimitive, Conc {
}
