package ib053.core;

import com.esotericsoftware.jsonbeans.JsonValue;

/**
 * Represents an enemy type
 */
public final class Enemy implements AttributeHolder {
    /** Unique id of this enemy type */
    public final long id;
    /** Display name of this enemy */
    public final String name;
    /** Description of this enemy (not null) */
    public final String description;

    /** Combat attributes of this enemy */
    public final Attributes attributes;

    private Enemy(long id, String name, String description, Attributes attributes) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.attributes = attributes;
    }

    public static Enemy read(JsonValue jsonValue) {
        final long id = jsonValue.getLong("id");
        final String name = jsonValue.getString("name");
        final String description = jsonValue.getString("description");

        final Attributes attributes = Attributes.read(jsonValue.get("attributes"), false);

        return new Enemy(id, name, description, attributes);
    }

    @Override
    public int get(Attribute attribute) {
        return attributes.get(attribute);
    }
}
