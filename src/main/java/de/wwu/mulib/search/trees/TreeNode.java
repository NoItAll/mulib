package de.wwu.mulib.search.trees;

public abstract class TreeNode {
    public final Choice.ChoiceOption parent;
    public final int depth;

    public TreeNode(Choice.ChoiceOption parent) {
        this.parent = parent;
        if (parent != null) {
            parent.setChild(this);
            this.depth = parent.getDepth() + 1;
        } else {
            this.depth = 0;
        }
    }
}
