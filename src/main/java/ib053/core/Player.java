package ib053.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Represents the player and their character.
 *
 * Contains only pure getters/setters, for fields that need them.
 */
public final class Player implements AttributeHolder {

    private final GameCore core;
    private final long id;
    private final String name;

    public final Attributes attributes;
    /** Use {@link GameCore#giveExperience(Player, int)} to give experience. */
    public int experience = 0;
    public int virtuePoints = 0;
    public int health;

    private final EnumMap<Item.ItemType, Item> equipment = new EnumMap<>(Item.ItemType.class);
    /** Inventory contents. Does not contain equipment, that is in {@link #equipment} */
    private final List<Item> inventory = new ArrayList<>();

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

    public int getExperience() {
        return experience;
    }

    public Item getEquipment(Item.ItemType type) {
        if (!type.canEquip) return null;
        return equipment.get(type);
    }

    /** Returns old equipped item if any, or given item if that item can't be equipped. */
    public Item setEquipment(Item item) {
        if (item.type.canEquip) {
            return equipment.put(item.type, item);
        } else {
            return item;
        }
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
        if (attribute.type == Attribute.AttributeType.CHARACTER) {
            return this.attributes.get(attribute);
        }

        final Attributes combinedAttributes = Attributes.combinationOf(this.attributes);
        for (Item item : equipment.values()) {
            combinedAttributes.and(item.attributes);
        }
        return combinedAttributes.get(attribute);
    }
}
