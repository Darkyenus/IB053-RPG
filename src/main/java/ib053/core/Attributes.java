package ib053.core;

import com.esotericsoftware.jsonbeans.JsonValue;

/**
 *
 */
public final class Attributes {

    private final int[] modifiers = new int[Attribute.VALUES.length];

    public int get(Attribute attribute) {
        return modifiers[attribute.ordinal()];
    }

    public static Attributes read(JsonValue jsonValue) {
        final Attributes attributes = new Attributes();
        for (Attribute attribute : Attribute.VALUES) {
            attributes.modifiers[attribute.ordinal()] = jsonValue.getInt(attribute.shortName, 0);
        }
        return attributes;
    }
}
