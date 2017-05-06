package ib053.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the activity the player is doing and fully handles interaction with them.
 *
 * Subclassed for each activity.
 * One instance of subclass per activity, not per player, if possible.
 */
public abstract class Activity {

    /** List of actions currently available to players engaged in this actions.
     * May be changed only through {@link GameCore#setActivityActions(Activity, List)} */
    final List<Action> actions = new ArrayList<>();

    /** List of players engaged in this activity. Modifiable only through {@link GameCore#changePlayerActivity(Player, Activity)} */
    final List<Player> engagedPlayers = new ArrayList<>();

    GameCore core = null;

    /** Get core to which this Activity has been initialized.
     * Valid to call only after {@link #initialize()} has been called!!! */
    public final GameCore getCore() {
        assert core != null : "Activity not initialized yet!";
        return core;
    }

    /** Called before activity is first assigned to any player. */
    public abstract void initialize();

    /** Called when player begins this activity. */
    public abstract void beginActivity(Player player);

    /** Get list of actions available to players engaged in this activity.
     * DO NOT MODIFY!!! */
    public final List<Action> getActions() {
        return actions;
    }

    /** Called when player asks for description of the action. */
    public abstract String getDescription(Player player);

    /** Called when player ends this activity. */
    public abstract void endActivity(Player player);
}
