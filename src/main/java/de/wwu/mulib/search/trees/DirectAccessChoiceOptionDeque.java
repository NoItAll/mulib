package de.wwu.mulib.search.trees;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DirectAccessChoiceOptionDeque implements ChoiceOptionDeque {
    private final ArrayList<ChoiceOptionLevelContainer> choiceOptions;
    private final int batchIncreaseOfDirectAccessList = 32;
    private int size = 0;
    private int maxDepth = 0;

    protected DirectAccessChoiceOptionDeque(Choice.ChoiceOption rootChoice) {
        this.choiceOptions = new ArrayList<>();
        ChoiceOptionLevelContainer initialContainer = new ChoiceOptionLevelContainer(rootChoice.getDepth());
        initialContainer.insert(rootChoice);
        size++;
        this.maxDepth = rootChoice.getDepth();
        this.choiceOptions.add(initialContainer);
        cachedTail = initialContainer;
        cachedHead = initialContainer;
    }

    @Override
    public synchronized void setEmpty() {
        choiceOptions.clear();
    }

    private volatile ChoiceOptionLevelContainer cachedHead;
    // Returns a locked ChoiceOptionLevelContainer
    private ChoiceOptionLevelContainer getHead() {
        ChoiceOptionLevelContainer cachedContainer = cachedHead;
        if (cachedContainer.isEmpty()) {
            // New head must be read
            for (int i = cachedContainer.depth; i < choiceOptions.size(); i++) {
                ChoiceOptionLevelContainer current = choiceOptions.get(i);
                if (!current.isEmpty()) {
                    cachedHead = current;
                    return current;
                }
            }
            return null;
        } else {
            return cachedContainer;
        }
    }

    private volatile ChoiceOptionLevelContainer cachedTail;
    // Returns a locked ChoiceOptionLevelContainer
    private ChoiceOptionLevelContainer getTail() {
        ChoiceOptionLevelContainer cachedContainer = cachedTail;
        if (cachedContainer.isEmpty()) {
            // New head must be read
            ChoiceOptionLevelContainer possibleNewContainer = null;
            for (int i = cachedContainer.depth; i >= cachedHead.depth; i--) {
                ChoiceOptionLevelContainer current = choiceOptions.get(i);
                if (!current.isEmpty()) {
                    possibleNewContainer = current;
                    break;
                }
            }
            if (possibleNewContainer != null) {
                cachedTail = possibleNewContainer;
                return possibleNewContainer;
            }
            return null;
        } else {
            return cachedContainer;
        }
    }

    @Override
    public synchronized Optional<Choice.ChoiceOption> pollFirst() {
        return poll(false);
    }

    @Override
    public synchronized Optional<Choice.ChoiceOption> pollLast() {
        return poll(true);
    }

    private Optional<Choice.ChoiceOption> poll(boolean last) {
        ChoiceOptionLevelContainer levelChoiceOptions = last ? getTail() : getHead();
        if (levelChoiceOptions == null) {
            return Optional.empty();
        }
        Choice.ChoiceOption result = levelChoiceOptions.poll();
        if (result == null) {
            return poll(last);
        } else {
            size--;
            return Optional.of(result);
        }
    }

    @Override
    public synchronized void insert(int depth, List<Choice.ChoiceOption> newChoiceOptions) {
        ensureInitializedDepth(depth);
        if (depth > cachedTail.depth) {
            cachedTail = choiceOptions.get(depth);
        }
        if (cachedHead.depth > depth) {
            cachedHead = choiceOptions.get(depth);
        }
        if (depth > this.maxDepth) {
            this.maxDepth = depth;
        }
        ChoiceOptionLevelContainer toAddTo = choiceOptions.get(depth);
        for (Choice.ChoiceOption co : newChoiceOptions) {
            if (!co.isUnsatisfiable()) {
                size++;
                toAddTo.insert(co);
            }
        }
    }

    private void ensureInitializedDepth(int depth) {
        if (choiceOptions.size() <= depth) {
            int currentSize = choiceOptions.size();
            for (int i = currentSize; i <= depth + batchIncreaseOfDirectAccessList; i++) {
                choiceOptions.add(new ChoiceOptionLevelContainer(i));
            }
        }
    }

    @Override
    public synchronized boolean isEmpty() {
        return getHead() == null;
    }

    @Override
    public synchronized boolean request(Choice.ChoiceOption toRemove) {
        ChoiceOptionLevelContainer container = choiceOptions.get(toRemove.getDepth());
        if (container.remove(toRemove)) {
            size--;
            return true;
        }
        return false;
    }

    @Override
    public synchronized int size() {
        return size;
    }

    @Override
    public int maxDepth() {
        return maxDepth;
    }

}
