package ib053.core;

import com.esotericsoftware.jsonbeans.JsonValue;
import com.koloboke.collect.map.ObjLongMap;
import com.koloboke.collect.map.hash.HashObjLongMaps;

/**
 * Represents single location type.
 */
public final class Location {
    /** Unique location id */
    public final long id;
    /** Display name of this location */
    public final String name;
    /** Description of this location (not null) */
    public final String description;
    /** Map of "description of path" -> location id */
    public final ObjLongMap<String> directions;

    /** Location id of a location which is graveyard for this location.
     * Defaults to self. */
    public final long graveyardId;

    /** Two-array map of enemyId -> enemyRarity. Enemy rarity works as follows:
     * When player enters a location, all enemies with rarity > 1 are considered in random order.
     * For each of them, random number [1,2) is generated, and if this number is smaller than that enemy's rarity,
     * fight with that enemy begins.
     *
     * Otherwise, when player is looking for a fight, 1 is subtracted from rarities in [1,2] range
     * and all rarities are used as weights in random selection. */
    private final long[] enemies;
    private final float[] enemyRarities;
    private final float baseEnemyRaritiesSum;

    /** Helper array holding indices into enemies and enemyRarities.
     * Used when choosing an enemy. */
    private final int[] _enemyTraversalPermutation;

    private Location(long id, String name, String description, ObjLongMap<String> directions, long graveyardId, long[] enemies, float[] enemyRarities) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.directions = directions;
        this.graveyardId = graveyardId;
        this.enemies = enemies;
        this.enemyRarities = enemyRarities;

        float baseEnemyRaritiesSum = 0;
        for (float enemyRarity : enemyRarities) {
            if (enemyRarity > 1f) {
                baseEnemyRaritiesSum += enemyRarity - 1f;
            } else {
                baseEnemyRaritiesSum += enemyRarity;
            }
        }
        this.baseEnemyRaritiesSum = baseEnemyRaritiesSum;

        int[] enemyTraversalPermutation = enemies.length == 0 ? NO_ENEMY_TRAVERSAL_PERMUTATION : new int[enemies.length];
        for (int i = 0; i < enemyTraversalPermutation.length; i++) {
            enemyTraversalPermutation[i] = i;
        }
        this._enemyTraversalPermutation = enemyTraversalPermutation;
    }

    private int[] enemyPermutation() {
        final int[] p = _enemyTraversalPermutation;
        RandomUtil.shuffle(p);
        return p;
    }

    public boolean hasEnemies() {
        return enemyRarities.length > 0 && baseEnemyRaritiesSum > 0f;
    }

    public long selectEnemyToFightOnEntry() {
        final float[] enemyRarities = this.enemyRarities;
        if (enemyRarities.length == 0) return -1;

        for (int enemyIndex : enemyPermutation()) {
            if (enemyRarities[enemyIndex] <= 1f) continue;
            if ((enemyRarities[enemyIndex] - 1f) > RandomUtil.RANDOM.nextFloat()) return enemies[enemyIndex];
        }

        return -1;
    }

    public long selectEnemyToFight() {
        final float[] enemyRarities = this.enemyRarities;
        if (enemyRarities.length == 0) return -1;

        float remainingWeight = RandomUtil.RANDOM.nextFloat() * baseEnemyRaritiesSum;
        for (int enemyIndex : enemyPermutation()) {
            remainingWeight -= enemyRarities[enemyIndex];
            if (remainingWeight <= 0f) {
                return enemies[enemyIndex];
            }
        }

        assert false : "This should not happen";
        return enemies[0];
    }

    private static final long[] NO_ENEMIES = new long[0];
    private static final float[] NO_ENEMY_RARITIES = new float[0];
    private static final int[] NO_ENEMY_TRAVERSAL_PERMUTATION = new int[0];

    public static Location read(JsonValue jsonValue) {
        final long id = jsonValue.getLong("id");
        final String name = jsonValue.getString("name");
        final String description = jsonValue.getString("description", "");

        final long graveyardId = jsonValue.getLong("graveyard", id);

        final ObjLongMap<String> directions = HashObjLongMaps.newImmutableMap((map) -> {
            for (JsonValue direction : jsonValue.get("directions")) {
                map.accept(direction.name(), direction.asLong());
            }
        });

        long[] enemies = NO_ENEMIES;
        float[] enemyRarities = NO_ENEMY_RARITIES;
        {
            //Enemies are stored:
            /*
            enemies: {
                "1":0.5,
                "3":1.2
            }
             */
            // Which means: enemy with id 1 has weight 0.5 and enemy with id 3 has weight 0.2,
            // but also 20% chance of appearing on entry
            final JsonValue enemiesJson = jsonValue.get("enemies");
            if (enemiesJson != null && enemiesJson.size != 0) {
                enemies = new long[enemiesJson.size];
                enemyRarities = new float[enemiesJson.size];

                int i = 0;
                JsonValue pair = enemiesJson.child();
                while (pair != null) {
                    enemies[i] = Long.parseLong(pair.name());
                    enemyRarities[i] = pair.asFloat();
                    i++;
                    pair = pair.next();
                }
            }
        }

        return new Location(id, name, description, directions, graveyardId, enemies, enemyRarities);
    }
}
