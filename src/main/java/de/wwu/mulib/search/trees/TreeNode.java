package de.wwu.mulib.search.trees;

/**
 * Abstract supertype for all tree nodes.
 * Solely the root node in a search tree has no parent.
 */
public abstract class TreeNode {
    /**
     * The parent of the tree node
     */
    public final Choice.ChoiceOption parent;
    /**
     * The depth of the tree node
     */
    public final int depth;

    TreeNode(Choice.ChoiceOption parent) {
        this.parent = parent;
        if (parent != null) {
            parent.setChild(this);
            this.depth = parent.getDepth() + 1;
        } else {
            this.depth = 1;
        }
    }
}
