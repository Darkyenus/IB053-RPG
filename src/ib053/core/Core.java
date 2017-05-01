package ib053.core;

import java.util.UUID;

/**
 * Interface for clearly specifying interface between core and frontends.
 */
public interface Core {

    /** Creates a whole new Player, with given name. */
    Player createNewPlayer(String name);

    /** @return Player prevously created with given UUID, or null if no such player exists. */
    Player findPlayer(UUID id);

}
