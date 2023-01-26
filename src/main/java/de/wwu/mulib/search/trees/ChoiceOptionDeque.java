package de.wwu.mulib.search.trees;

import java.util.List;
import java.util.Optional;

public interface ChoiceOptionDeque {

    Optional<Choice.ChoiceOption> pollFirst();

    Optional<Choice.ChoiceOption> pollLast();

    void insert(int depth, List<Choice.ChoiceOption> choiceOptions);

    boolean isEmpty();

    boolean request(Choice.ChoiceOption toRemove);

    void setEmpty();

    int size();

    int[] minMaxZero = { 0, 0 };
    int[] getMinMaxDepth();
}
