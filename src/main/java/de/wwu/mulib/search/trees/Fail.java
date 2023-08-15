package de.wwu.mulib.search.trees;

/**
 * A node indicating that the execution failed
 */
public class Fail extends TreeNode {

    /**
     * If this is true, the fail was thrown by the user. Otherwise, the exploration of this path ended because the
     * constraint system has become unsatisfiable
     */
    public final boolean explicitlyFailed;

    Fail(Choice.ChoiceOption parent, boolean explicitlyFailed) {
        super(parent);
        this.explicitlyFailed = explicitlyFailed;
    }

    @Override
    public String toString() {
        return "Fail{depth=" + depth + ",explicitlyFailed=" + explicitlyFailed + "}";
    }
}
