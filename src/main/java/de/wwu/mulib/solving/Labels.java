package de.wwu.mulib.solving;

import de.wwu.mulib.substitutions.SubstitutedVar;

import java.util.Collection;
import java.util.Map;

/**
 * An implementation of the container used for storing labels.
 * The name for the return value is always "return"
 */
public interface Labels {

    /**
     * Returns the object for the name used for remembering
     * @param id The name used for remembering
     * @return The object with a non-search region representation
     */
    Object getLabelForId(String id);

    /**
     * Returns the search region-representation of the object used in the search region that was remembered
     * @param id The name used for remembering
     * @return The object with a search region representation
     */
    SubstitutedVar getNamedVar(String id);

    /**
     * @return A map representation of the (remembering name, search region representation)-pairs
     */
    Map<String, SubstitutedVar> getIdToNamedVar();

    /**
     * @return A map representation of the (remembering name, label)-pairs
     */
    Map<String, Object> getIdToLabel();

    /**
     * @return A collection with the names used for remembering
     */
    Collection<String> getNames();
}
