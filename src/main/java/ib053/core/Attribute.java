package ib053.core;

/**
 *
 */
public enum Attribute {
    LEVEL("LVL", AttributeType.CHARACTER),

    STRENGTH("STR", AttributeType.VIRTUE),
    DEXTERITY("DEX", AttributeType.VIRTUE),
    AGILITY("AGI", AttributeType.VIRTUE),
    LUCK("LUCK", AttributeType.VIRTUE),
    STAMINA("STA", AttributeType.VIRTUE),

    DAMAGE("DMG", AttributeType.WEAPON),
    ARMOR("AMR", AttributeType.ARMOR),
    ;

    public final String shortName;
    public final AttributeType type;

    Attribute(String shortName, AttributeType type) {
        this.shortName = shortName;
        this.type = type;
    }

    public static final Attribute[] VALUES = values();

    public enum AttributeType {
        VIRTUE,
        WEAPON,
        ARMOR,
        CHARACTER
    }
}
