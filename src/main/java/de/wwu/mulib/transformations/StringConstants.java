package de.wwu.mulib.transformations;

/**
 * Class defining some useful string constants
 */
public final class StringConstants {
    private StringConstants() {}

    /**
     * The String indicating that something has a special meaning in the search region.
     * All partner classes are prefixed by this
     */
    public static final String _TRANSFORMATION_INDICATOR = "__mulib__";
    /**
     * Name of synthesized getter-accessor method that is used instead of getting the field directly
     */
    public static final String _ACCESSOR_PREFIX = "get" + _TRANSFORMATION_INDICATOR;
    /**
     * Name of synthesized getter-accessor method that is used instead of setting the field directly
     */
    public static final String _SETTER_PREFIX = "set" + _TRANSFORMATION_INDICATOR;
    public static final String clinit = "<clinit>";
    public static final String init = "<init>";
    public static final String main = "main";
    /**
     * The postfix used for generated sarrays of sarray
     */
    public static final String _SARRAYSARRAY_POSTFIX = _TRANSFORMATION_INDICATOR + "SarraySarray";
    /**
     * The postfix used for generated array of other objects
     */
    public static final String _PARTNER_CLASSSARRAY_POSTFIX = _TRANSFORMATION_INDICATOR + "PartnerClassSarray";
    public static final String primitiveTypes = "BCDFIJSZ";
}
