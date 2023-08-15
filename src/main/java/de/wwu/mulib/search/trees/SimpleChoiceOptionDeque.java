package de.wwu.mulib.search.trees;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A deque based on a linked list.
 * Oftentimes offers a bad performance for {@link de.wwu.mulib.search.executors.SearchStrategy#DSAS} and
 * {@link de.wwu.mulib.search.executors.SearchStrategy#IDDSAS}.
 */
public class SimpleChoiceOptionDeque implements ChoiceOptionDeque {

    private final LinkedList<Choice.ChoiceOption> choiceOptions;

    protected SimpleChoiceOptionDeque(Choice.ChoiceOption rootChoice) {
        this.choiceOptions = new LinkedList<>();
        this.choiceOptions.add(rootChoice);
    }

    @Override
    public synchronized void setEmpty() {
        choiceOptions.clear();
    }

    @Override
    public synchronized boolean request(
            Choice.ChoiceOption requested) {
        return choiceOptions.remove(requested);
    }

    @Override
    public synchronized Optional<Choice.ChoiceOption> pollFirst() {
        return get(choiceOptions::pollFirst);
    }

    private Optional<Choice.ChoiceOption> get(Supplier<Choice.ChoiceOption> co) {
        Choice.ChoiceOption result = co.get();
        if (result != null) {
            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public synchronized Optional<Choice.ChoiceOption> pollLast() {
        return get(choiceOptions::pollLast);
    }

    @Override
    public synchronized void insert(int depth, List<Choice.ChoiceOption> options) {
        if (choiceOptions.isEmpty()) {
            List<Choice.ChoiceOption> filtered = new ArrayList<>(options);
            filtered.removeIf(Choice.ChoiceOption::isUnsatisfiable);
            choiceOptions.addAll(filtered);
        } else if (depth >= choiceOptions.getLast().getDepth()) {
            // Add as the new tail
            addToList(options, choiceOptions::addLast);
        } else if (depth <= choiceOptions.getFirst().getDepth()) {
            // Add as new head
            addToList(options, choiceOptions::addFirst);
        } else {
            List<Choice.ChoiceOption> filtered = new ArrayList<>(options);
            filtered.removeIf(Choice.ChoiceOption::isUnsatisfiable);
            // Add in-between
            int i = 0;
            for (Choice.ChoiceOption co : choiceOptions) {
                if (co.getDepth() > depth) {
                    choiceOptions.addAll(i, filtered);
                    break;
                }
                i++;
            }
        }
    }

    @Override
    public synchronized boolean isEmpty() {
        return choiceOptions.isEmpty();
    }

    private void addToList(
            List<Choice.ChoiceOption> choiceOptions,
            Consumer<Choice.ChoiceOption> consumerFunction) {
        for (Choice.ChoiceOption co : choiceOptions) {
            if (!co.isUnsatisfiable()) {
                consumerFunction.accept(co);
            }
        }
    }

    @Override
    public synchronized String toString() {
        return "SimpleChoiceOptionDeque{"
                + "size=" + choiceOptions.size()
                + ",choiceOptions=" + choiceOptions
                + "}";
    }

    @Override
    public synchronized int size() {
        return choiceOptions.size();
    }

    @Override
    public synchronized int[] getMinMaxDepth() {
        if (!choiceOptions.isEmpty()) {
            return new int[] { choiceOptions.getFirst().getDepth(), choiceOptions.getLast().getDepth() };
        }
        return minMaxZero;
    }

}
