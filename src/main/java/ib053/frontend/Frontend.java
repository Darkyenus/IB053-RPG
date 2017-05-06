package ib053.frontend;

import ib053.core.GameCore;
import ib053.core.Event;
import ib053.core.Player;

/**
 * Represents a frontend - service which communicates with players/humans.
 *
 * All methods are called from the GameCore's event loop - they should not block.
 * Received objects are safe to manipulate only from the GameCore's event loop thread, unless noted otherwise.
 */
public interface Frontend {

    /** Initialize this frontend to given GameCore. */
    void initialize(GameCore core);

    /** Begin interfacing with clients */
    void begin();

    /** Called when something about player's activity changes, be it actions or display. */
    void playerActivityChanged(Player player);

    /** Called when player receives an event (player's character witnesses something or something happens to them) */
    void playerReceiveEvent(Player player, Event event);
}
