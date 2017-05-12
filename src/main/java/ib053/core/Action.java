package ib053.core;

/**
 * Represents a single action which the user can do.
 */
public abstract class Action {

    /** Activity to which this action belongs. */
    public final ActivityBase activity;

    /** Display name of the action group. Actions of the same group should be displayed together.
     * May be null for general group. */
    public final String group;

    /** Display name of the action. */
    public final String name;

    protected Action(ActivityBase activity, String group, String name) {
        assert activity != null;
        assert name != null;

        this.activity = activity;
        this.group = group;
        this.name = name;
    }

    /** Make the given player perform this action.
     * @return true if the action was performed, false if it didn't pass checks. */
    public final boolean perform(Player player) {
        if(player.currentActivity != activity || !activity.actions.contains(this)) {
            return false;
        }

        performAction(player);
        return true;
    }

    /** Make the given player perform this action. Called by {@link #perform(Player)} after validity checks. */
    protected abstract void performAction(Player player);
}
