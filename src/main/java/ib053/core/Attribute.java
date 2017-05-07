package ib053.core;

/**
 *
 */
public enum Attribute {
    /** Does not modify anything directly, but each level adds 2 virtue points for character
     * to redistribute to their attributes (not luck) */
    LEVEL("LVL", AttributeType.CHARACTER, 1, Integer.MAX_VALUE),

    /** Increases damage multiplier from damage of weapon */
    STRENGTH("STR", AttributeType.VIRTUE, 0, Integer.MAX_VALUE),
    /** Ratio of attacker and defender dexterity determine hit chance. */
    DEXTERITY("DEX", AttributeType.VIRTUE),
    AGILITY("AGI", AttributeType.VIRTUE),
    /** Increases chance of something good happening (and decreases this change for the opponent).
     * 100 my luck and 0 opponent luck => 60% chance of something good happening.
     * 100 my luck and 100 opponent luck => 20% chance of something good happening.
     * 100 my luck and no opponent => 50% chance of something good happening.
     * 0 my luck => 0% chance of something good happening. */
    LUCK("LUCK", AttributeType.VIRTUE, 0, 100),
    /** 1 stamina point <=> +5max HP */
    STAMINA("STA", AttributeType.VIRTUE),

    /** Base damage of a weapon (or enemy) */
    DAMAGE("DMG", AttributeType.WEAPON),
    /** Damage of weapon in a hit is in range [DMG - DMG RANGE; DMG + DMG RANGE],
     * distributed using gaussian or similar distribution.*/
    DAMAGE_SPREAD("DMG RANGE", AttributeType.WEAPON),
    ARMOR("ARMOR", AttributeType.ARMOR),
    ;

    public final String shortName;
    public final AttributeType type;
    public final int minValue, maxValue;

    Attribute(String shortName, AttributeType type, int minValue, int maxValue) {
        this.shortName = shortName;
        this.type = type;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    Attribute(String shortName, AttributeType type) {
        this(shortName, type, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static final Attribute[] VALUES = values();

    public enum AttributeType {
        VIRTUE,
        WEAPON,
        ARMOR,
        CHARACTER
    }
}
