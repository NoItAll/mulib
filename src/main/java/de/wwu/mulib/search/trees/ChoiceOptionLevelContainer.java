package de.wwu.mulib.search.trees;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ChoiceOptionLevelContainer {
    @SuppressWarnings("all")
    final int depth;
    private final LinkedList<Choice.ChoiceOption> choiceOptionsOfDepth;

    public ChoiceOptionLevelContainer(int depth) {
        this.depth = depth;
        this.choiceOptionsOfDepth = new LinkedList<>();
    }

    public void insert(Choice.ChoiceOption co) {
        assert co.getDepth() == depth;
        choiceOptionsOfDepth.add(co);
    }

    public Choice.ChoiceOption poll() {
        return choiceOptionsOfDepth.poll();
    }

    public boolean isEmpty() {
        return choiceOptionsOfDepth.isEmpty();
    }

    public List<Choice.ChoiceOption> getChoiceOptions() {
        return choiceOptionsOfDepth;
    }

    public boolean remove(Choice.ChoiceOption toRemove) {
        return choiceOptionsOfDepth.remove(toRemove);
    }
}
