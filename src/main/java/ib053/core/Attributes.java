package ib053.core;

import com.esotericsoftware.jsonbeans.JsonValue;

/**
 * Holds attributes for item/player/enemy/etc.
 *
 * Not all attributes make sense for all attribute holders, however this is not checked (yet).
 */
public final class Attributes implements AttributeHolder {

    private final int[] modifiers = new int[Attribute.VALUES.length];
    private final boolean mutable;

    public Attributes(boolean mutable) {
        this.mutable = mutable;
    }

    /** @return value of attribute, unclamped */
    public int getRaw(Attribute attribute) {
        assert attribute != null;
        return modifiers[attribute.ordinal()];
    }

    /** @return value of attribute, clamped to valid range */
    public int get(Attribute attribute) {
        assert attribute != null;
        final int value = modifiers[attribute.ordinal()];
        if (value < attribute.minValue) {
            return attribute.minValue;
        } else if (value > attribute.maxValue) {
            return attribute.maxValue;
        } else {
            return value;
        }
    }

    /** Set value of given attribute.
     * Valid call only on mutable Attributes.
     * Stored value is not clamped. */
    public void set(Attribute attribute, int value) {
        assert attribute != null;
        if (!mutable) throw new UnsupportedOperationException("Can't set attribute of immutable Attributes");
        modifiers[attribute.ordinal()] = value;
    }

    /** Add to value of given attribute.
     * Valid call only on mutable Attributes.
     * Stored value is not clamped.
     * @return new value */
    public int add(Attribute attribute, int value) {
        assert attribute != null;
        if (!mutable) throw new UnsupportedOperationException("Can't set attribute of immutable Attributes");
        modifiers[attribute.ordinal()] += value;
        return get(attribute);
    }

    /** Creates a mutable copy of given base, use with {@link #and(Attributes)} */
    public static Attributes combinationOf(Attributes base) {
        final Attributes result = new Attributes(true);
        System.arraycopy(base.modifiers, 0, result.modifiers, 0, Attribute.VALUES.length);
        return result;
    }

    /** Adds other attributes into these attributes and returns this */
    public Attributes and(Attributes other) {
        assert other != null;
        if (!mutable) throw new UnsupportedOperationException("Can't set attribute of immutable Attributes");

        for (int i = 0; i < Attribute.VALUES.length; i++) {
            this.modifiers[i] += other.modifiers[i];
        }

        return this;
    }

    public static Attributes read(JsonValue jsonValue, boolean mutable) {
        final Attributes attributes = new Attributes(mutable);
        for (Attribute attribute : Attribute.VALUES) {
            attributes.modifiers[attribute.ordinal()] = jsonValue.getInt(attribute.shortName, 0);
        }
        return attributes;
    }
}
