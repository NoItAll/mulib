package de.wwu.mulib.search.trees;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.NotYetImplementedException;

/**
 * Specifies the type of deque supported by Mulib
 */
public enum ChoiceOptionDeques {
    /**
     * A deque based on a linked list.
     * Oftentimes offers a bad performance for {@link de.wwu.mulib.search.executors.SearchStrategy#DSAS} and
     * {@link de.wwu.mulib.search.executors.SearchStrategy#IDDSAS}.
     */
    SIMPLE,
    /**
     * A more complex deque based on a list of level containers supporting accessing the sublist of
     * {@link de.wwu.mulib.search.trees.Choice.ChoiceOption} for a given depth.
     */
    DIRECT_ACCESS;


    static ChoiceOptionDeque getChoiceOptionDeque(MulibConfig config, Choice.ChoiceOption rootOption) {
        switch (config.SEARCH_CHOICE_OPTION_DEQUE_TYPE) {
            case SIMPLE:
                return new SimpleChoiceOptionDeque(rootOption);
            case DIRECT_ACCESS:
                return new DirectAccessChoiceOptionDeque(rootOption);
            default:
                throw new NotYetImplementedException();
        }
    }
}
