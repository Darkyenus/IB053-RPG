package ib053.core;

import java.util.*;

/**
 *
 */
public final class Place {

    private final GameCore core;
    private final long id;
    private final String name;
    private final String flavorText;

    final Map<String, Place> directions = new HashMap<>();
    /** Players currently in this area. Do not modify: {@link GameCore#movePlayer(Player, Place)} */
    final List<Player> presentPlayers = new ArrayList<>();

    public GameCore getCore() {
        return core;
    }

    /** Unique ID of this player. Use for persistency. */
    public long getId() {
        return id;
    }

    /** Name of this place. Use for UI. */
    public String getName() {
        return name;
    }

    /** Short description of this place, use for UI when entering the area. */
    public String getFlavorText() {
        return flavorText;
    }

    /** List of places reachable from here, for example "North" -> Forest */
    public Map<String, Place> getDirections() {
        return directions;
    }

    /** Collection of players currently in this place. Do not modify. */
    public Collection<Player> getPresentPlayers() {
        return presentPlayers;
    }

    Place(GameCore core, long id, String name, String flavorText) {
        this.core = core;
        this.id = id;
        this.name = name;
        this.flavorText = flavorText;
    }
}
