package ib053.core;

import java.util.UUID;

/**
 * Represents connected player. Just an identifier.
 *
 * Players are created by the core and backend takes care of distributing Player instances to humans.
 */
public final class Player {

    private final UUID id;
    private String name;

    public Player(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getID() {
        return id;
    }

    /** @return Name of the character this player plays as. */
    public String getName() {
        return name;
    }

    /** Used only by the core. */
    protected void setName(String name) {
        this.name = name;
    }
}
