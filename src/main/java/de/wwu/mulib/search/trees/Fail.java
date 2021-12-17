package de.wwu.mulib.search.trees;

public class Fail extends TreeNode {

    public final boolean explicitlyFailed;

    public Fail(Choice.ChoiceOption parent, boolean explicitlyFailed) {
        super(parent);
        this.explicitlyFailed = explicitlyFailed;
    }

    @Override
    public String toString() {
        return "Fail{depth=" + depth + ",explicitlyFailed=" + explicitlyFailed + "}";
    }
}
