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

    private Location(long id, String name, String description, ObjLongMap<String> directions) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.directions = directions;
    }

    public static Location read(JsonValue jsonValue) {
        final long id = jsonValue.getLong("id");
        final String name = jsonValue.getString("name");
        final String description = jsonValue.getString("description", "");

        final ObjLongMap<String> directions = HashObjLongMaps.newImmutableMap((map) -> {
            for (JsonValue direction : jsonValue.get("directions")) {
                map.accept(direction.name(), direction.asLong());
            }
        });

        return new Location(id, name, description, directions);
    }
}
