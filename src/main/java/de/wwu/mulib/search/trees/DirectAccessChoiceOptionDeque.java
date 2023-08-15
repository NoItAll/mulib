package de.wwu.mulib.search.trees;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * A more complex deque based on a list of level containers supporting accessing the sublist of
 * {@link de.wwu.mulib.search.trees.Choice.ChoiceOption} for a given depth.
 */
public class DirectAccessChoiceOptionDeque implements ChoiceOptionDeque {
    private final ArrayList<ChoiceOptionLevelContainer> choiceOptions;
    private final int batchIncreaseOfDirectAccessList = 32;
    private int size = 0;

    protected DirectAccessChoiceOptionDeque(Choice.ChoiceOption rootChoice) {
        this.choiceOptions = new ArrayList<>();
        ChoiceOptionLevelContainer initialContainer = new ChoiceOptionLevelContainer(rootChoice.getDepth());
        initialContainer.insert(rootChoice);
        this.size++;
        this.choiceOptions.add(initialContainer);
        this.cachedTail = initialContainer;
        this.cachedHead = initialContainer;
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
    public synchronized boolean request(Choice.ChoiceOption requested) {
        ChoiceOptionLevelContainer container = choiceOptions.get(requested.getDepth());
        if (container.remove(requested)) {
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
    public synchronized int[] getMinMaxDepth() {
        ChoiceOptionLevelContainer head = getHead();
        if (head != null) {
            ChoiceOptionLevelContainer tail = getTail();
            assert tail != null;
            return new int[] { head.depth, tail.depth };
        }
        return minMaxZero;
    }

    private static class ChoiceOptionLevelContainer {
        @SuppressWarnings("all")
        final int depth;
        private final LinkedList<Choice.ChoiceOption> choiceOptionsOfDepth;

        ChoiceOptionLevelContainer(int depth) {
            this.depth = depth;
            this.choiceOptionsOfDepth = new LinkedList<>();
        }

        void insert(Choice.ChoiceOption co) {
            assert co.getDepth() == depth;
            choiceOptionsOfDepth.add(co);
        }

        Choice.ChoiceOption poll() {
            return choiceOptionsOfDepth.poll();
        }

        boolean isEmpty() {
            return choiceOptionsOfDepth.isEmpty();
        }

        List<Choice.ChoiceOption> getChoiceOptions() {
            return choiceOptionsOfDepth;
        }

        boolean remove(Choice.ChoiceOption toRemove) {
            return choiceOptionsOfDepth.remove(toRemove);
        }
    }

}
