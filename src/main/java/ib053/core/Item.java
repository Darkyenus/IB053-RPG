package ib053.core;

import com.esotericsoftware.jsonbeans.Json;
import com.esotericsoftware.jsonbeans.JsonSerializer;
import com.esotericsoftware.jsonbeans.JsonValue;

/**
 * Represents single inventory item type.
 */
public final class Item {
    /** Unique item id */
    public final long id;
    /** Type of this item */
    public final ItemType type;
    /** Display name of this item */
    public final String name;
    /** Lore of this item, may be null (for most items) */
    public final String lore;

    /** Monetary value, may be positive, zero or {@link #VALUE_CANT_SELL} */
    public final int value;
    /** Attributes of this item, applied when worn */
    public final Attributes attributes;

    private Item(long id, ItemType type, String name, String lore, int value, Attributes attributes) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.lore = lore;
        this.value = value;
        this.attributes = attributes;
    }

    public static Item read(JsonValue jsonValue) {
        final long id = jsonValue.getLong("id");
        final ItemType type = ItemType.valueOf(jsonValue.getString("type").toUpperCase());
        final String name = jsonValue.getString("name");
        final String lore = jsonValue.getString("lore", null);

        final int value = jsonValue.getInt("value", 0);
        final Attributes attributes = Attributes.read(jsonValue.get("attributes"));

        //TODO Check invalid attributes and warn about them

        return new Item(id, type, name, lore, value, attributes);
    }

    public static final int VALUE_CANT_SELL = -1;

    public enum ItemType {
        WEAPON,
        ARMOR_HEAD,
        ARMOR_CHEST,
        ARMOR_LEGS,
        ARMOR_RING,
        SHIELD,
        JUNK
    }
}
