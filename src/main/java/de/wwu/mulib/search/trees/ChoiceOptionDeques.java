package de.wwu.mulib.search.trees;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.NotYetImplementedException;

public enum ChoiceOptionDeques {
    SIMPLE,
    DIRECT_ACCESS;


    public static ChoiceOptionDeque getChoiceOptionDeque(MulibConfig config, Choice.ChoiceOption rootOption) {
        switch (config.CHOICE_OPTION_DEQUE_TYPE) {
            case SIMPLE:
                return new SimpleChoiceOptionDeque(rootOption);
            case DIRECT_ACCESS:
                return new DirectAccessChoiceOptionDeque(rootOption);
            default:
                throw new NotYetImplementedException();
        }
    }
}
