package ib053.core;

/**
 * Thread-safe representation of one-time event player should be notified about,
 * but does not have to do anything with it.
 */
public final class Event {

    /** Event's message to player(s). */
    public final String message;

    public Event(String message) {
        this.message = message;
    }
}
