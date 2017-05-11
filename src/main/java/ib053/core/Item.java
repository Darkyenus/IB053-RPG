package ib053.core;

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

    public void toString(StringBuilder sb, CharSequence separator) {
        sb.append(name);
        if (value != VALUE_CANT_SELL) {
            sb.append(separator).append(value).append(" ¤");
        }
        if (lore != null) {
            sb.append(separator).append(lore);
        }
        for (Attribute attribute : Attribute.VALUES) {
            if (attribute.type == Attribute.AttributeType.CHARACTER) continue;
            final int value = attributes.get(attribute);
            if (value == 0) continue;
            sb.append(separator).append(attribute.shortName).append(": ").append(value);
        }
    }

    public static Item read(JsonValue jsonValue) {
        final long id = jsonValue.getLong("id");
        final ItemType type = ItemType.valueOf(jsonValue.getString("type").toUpperCase());
        final String name = jsonValue.getString("name");
        final String lore = jsonValue.getString("lore", null);

        final int value = jsonValue.getInt("value", 0);
        final Attributes attributes = Attributes.read(jsonValue.get("attributes"), false);

        //TODO Check invalid attributes and warn about them

        return new Item(id, type, name, lore, value, attributes);
    }

    public static final int VALUE_CANT_SELL = -1;

    public enum ItemType {
        WEAPON(true, "Weapon"),
        ARMOR_HEAD(true, "Head"),
        ARMOR_CHEST(true, "Chest"),
        ARMOR_LEGS(true, "Legs"),
        ARMOR_RING(true, "Ring"),
        SHIELD(true, "Shield"),
        JUNK(false, "Junk");

        public final boolean canEquip;
        public final String name;

        ItemType(boolean canEquip, String name) {
            this.canEquip = canEquip;
            this.name = name;
        }

        public static final ItemType[] VALUES = values();
    }
}
