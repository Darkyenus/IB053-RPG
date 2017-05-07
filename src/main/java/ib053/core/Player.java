package ib053.core;

/**
 * Represents the player and their character.
 */
public final class Player implements AttributeHolder {

    private final GameCore core;
    private final long id;
    private final String name;

    public final Attributes attributes;
    public int health;

    /** Do not modify.
     * @see GameCore#changePlayerActivity(Player, Activity) */
    Activity currentActivity;

    /** Do not modify.
     * @see GameCore#changePlayerLocation(Player, Location) */
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


    Player(GameCore core, long id, String name, Attributes attributes) {
        this.core = core;
        this.id = id;
        this.name = name;
        this.attributes = attributes;
        this.health = attributes.get(Attribute.STAMINA) * 5;
    }

    @Override
    public int get(Attribute attribute) {
        return attributes.get(attribute);
    }
}
