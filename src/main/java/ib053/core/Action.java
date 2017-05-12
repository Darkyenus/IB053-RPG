package ib053.core;

/**
 * Represents a single action which the user can do.
 */
public final class Action {

    /** Activity to which this action belongs. */
    public final ActivityBase activity;

    /** Unique, programmatic key which frontends can use to modify behavior.
     * In dot notation: "activity.group.name". Group may be omitted. */
    public final String key;

    /** Display name of the action group. Actions of the same group should be displayed together.
     * May be null for general group. */
    public final String group;

    /** Display name of the action. */
    public final String name;

    private boolean enabled = true;

    private final Perform perform;

    Action(ActivityBase activity, String key, String group, String name, Perform perform) {
        this.perform = perform;
        assert activity != null;
        assert name != null;

        this.key = key;
        this.activity = activity;
        this.group = group;
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (activity.core != null) {
                for (Player player : activity.engagedPlayers) {
                    activity.core.notifyPlayerActivityChanged(player);
                }
            }
        }
    }

    /** Make the given player perform this action.
     * @return true if the action was performed, false if it didn't pass checks. */
    public final boolean perform(Player player) {
        if(player.currentActivity != activity) {
            return false;
        }

        perform.perform(player);
        return true;
    }

    @FunctionalInterface
    public interface Perform {
        void perform(Player player);
    }
}
