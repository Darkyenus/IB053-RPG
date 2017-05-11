package ib053.core;

/**
 * Interface for things that have attributes (which may be combined (by simple sum) from multiple other AttributeHolders.
 *
 * Also contains some utility methods, which are meaningful only for certain attribute holders.
 */
public interface AttributeHolder {

    int get(Attribute attribute);


    default int getMaxHealth() {
        return get(Attribute.STAMINA) * 5;
    }

    /** Does a luck check, as described by {@link Attribute#LUCK}, against environment. */
    default boolean luckCheck() {
        return RandomUtil.check(get(Attribute.LUCK) * (0.5f / 100f));
    }

    /** Does a luck check, as described by {@link Attribute#LUCK}, against opponent.
     * @param opponentLuck clamped opponent luck */
    default boolean luckCheck(int opponentLuck) {
        float baseProbability = get(Attribute.LUCK) * (0.6f / 100f);
        float probabilityScale = opponentLuck * (0.8f / 100f);
        return RandomUtil.check(baseProbability * (1f - probabilityScale));
    }

    /** @see #luckCheck(int) convenience method for this */
    default boolean luckCheck(AttributeHolder opponent) {
        return luckCheck(opponent.get(Attribute.LUCK));
    }

    default int getXpToNextLevel() {
        final int level = get(Attribute.LEVEL);
        return (int) Math.ceil(Math.log10(level + 200) * Math.pow(level, 0.7) * 1.2 + 10);
    }
}
