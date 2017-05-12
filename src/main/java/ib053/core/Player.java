package ib053.core;

import com.esotericsoftware.jsonbeans.Json;
import com.esotericsoftware.jsonbeans.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Represents the player and their character.
 *
 * Contains only pure getters/setters, for fields that need them.
 */
public final class Player implements AttributeHolder {

    private static final Logger LOG = LoggerFactory.getLogger(Player.class);

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
     * @see GameCore#changePlayerActivity(Player, ActivityBase) */
    ActivityBase currentActivity;

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
    public ActivityBase getActivity() {
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

    static Player read(JsonValue value, GameCore core) throws Exception {
        final long id = value.getLong("id");
        final String name = value.getString("name");
        final Attributes attributes = Attributes.read(value.get("attributes"), true);

        final Player player = new Player(core, id, name, attributes);
        player.experience = value.getInt("experience");
        player.virtuePoints = value.getInt("virtuePoints");
        player.health = value.getInt("health");
        player.location = core.findLocation(value.getLong("locationId"));

        @SuppressWarnings("unchecked")
        final Class<? extends ActivityBase> activityClass = (Class<? extends ActivityBase>) Class.forName(value.getString("activityClass"));
        final ActivityType activityType = core.activityCache.getActivityType(activityClass);
        switch (activityType) {
            case SINGLETON_ACTIVITY:
                player.currentActivity = core.activityCache.getSingletonActivity(activityClass);
                break;
            case PER_LOCATION_ACTIVITY:
                player.currentActivity = core.activityCache.getLocationActivity(activityClass, player.location);
                break;
            case CUSTOM_ACTIVITY:
                ActivityBase activityBase = ActivityCache.instantiate(activityClass);
                final JsonValue activityData = value.get("activityData");
                if (activityBase instanceof ActivityBase.Serializable && activityData != null) {
                    ((ActivityBase.Serializable) activityBase).read(core, activityData);
                } else if (activityBase instanceof ActivityBase.Serializable) {
                    throw new IllegalArgumentException("Activity "+activityClass+" is serializable, but player "+id+" lacks activityData");
                } else if (activityData != null) {
                    LOG.warn("Player has activityData for "+activityClass+" but that is not serializable");
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid activity type "+activityType+" of "+activityClass);
        }

        for (JsonValue equipment : value.get("equipment")) {
            final long itemId = equipment.asLong();
            final Item item = core.findItem(itemId);
            if (item == null || !item.type.canEquip || player.equipment.containsKey(item.type)) {
                LOG.warn("Invalid equipment item id {}", itemId);
            } else {
                player.equipment.put(item.type, item);
            }
        }

        for (JsonValue inventory : value.get("inventory")) {
            final long itemId = inventory.asLong();
            final Item item = core.findItem(itemId);
            if (item == null) {
                LOG.warn("Invalid equipment item id {}", itemId);
            } else {
                player.inventory.add(item);
            }
        }

        return player;
    }

    static void write(Json json, Player player) {
        json.writeObjectStart();
        json.writeValue("id", player.id, long.class);
        json.writeValue("name", player.name, String.class);
        Attributes.write(json, "attributes", player.attributes);
        json.writeValue("experience", player.experience, int.class);
        json.writeValue("virtuePoints", player.virtuePoints, int.class);
        json.writeValue("health", player.health, int.class);
        json.writeValue("locationId", player.location.id, long.class);

        json.writeValue("activityClass", player.currentActivity.getClass().getName());
        final ActivityType activityType = player.core.activityCache.getActivityType(player.currentActivity.getClass());
        if (activityType == ActivityType.CUSTOM_ACTIVITY && player.currentActivity instanceof ActivityBase.Serializable) {
            json.writeObjectStart("activityData");
            ((ActivityBase.Serializable) player.currentActivity).write(json);
            json.writeObjectEnd();
        }


        json.writeArrayStart("equipment");
        for (Item equipped : player.equipment.values()) {
            json.writeValue(equipped.id, long.class);
        }
        json.writeArrayEnd();

        json.writeArrayStart("inventory");
        for (Item inventoryItem : player.inventory) {
            json.writeValue(inventoryItem.id, long.class);
        }
        json.writeArrayEnd();

        json.writeObjectEnd();
    }
}
