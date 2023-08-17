package de.wwu.mulib.substitutions.primitives;

/**
 * Marker interface for all symbolic values that do not represent a numeric expression but rather a leaf
 */
public interface SymSprimitiveLeaf extends SymSprimitive {

    /**
     * @return Some identifier of the leaf. Leafs with the same identifier equal
     */
    String getId();

}
