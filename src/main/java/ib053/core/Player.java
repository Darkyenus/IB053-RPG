package ib053.core;

/**
 * Represents the player and their character.
 */
public final class Player {

    private final GameCore core;
    private final long id;
    private final String name;

    /** Do not modify.
     * @see GameCore#changePlayerActivity(Player, Activity) */
    Activity currentActivity;

    /** Do not modify.
     * @see GameCore#movePlayer(Player, Location) */
    Location location;

    public GameCore getCore() {
        return core;
    }

    /** Unique ID of this player. Use for persistency. */
    public long getId() {
        return id;
    }

    /** Unique but not permanent name of this player. Use for UI. */
    public String getName() {
        return name;
    }

    /** Location in which the player resides. */
    public Location getLocation() {
        return location;
    }

    /** Activity which the player is performing */
    public Activity getActivity() {
        return currentActivity;
    }

    Player(GameCore core, long id, String name) {
        this.core = core;
        this.id = id;
        this.name = name;
    }
}
