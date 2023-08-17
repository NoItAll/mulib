package de.wwu.mulib.substitutions.primitives;

/**
 * Interface for all classes representing concrete numbers
 */
public interface ConcSnumber extends Snumber, ConcSprimitive {

    /**
     * @return The value (if transformed to an int)
     */
    int intVal();

    /**
     * @return The value (if transformed to a double)
     */
    double doubleVal();

    /**
     * @return The value (if transformed to a float)
     */
    float floatVal();

    /**
     * @return The value (if transformed to a long)
     */
    long longVal();

    /**
     * @return The value (if transformed to a short)
     */
    short shortVal();

    /**
     * @return The value (if transformed to a byte)
     */
    byte byteVal();

    /**
     * @return The value (if transformed to a char)
     */
    char charVal();

}
