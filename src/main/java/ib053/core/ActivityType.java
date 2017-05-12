package ib053.core;

/**
 * @see Activity
 */
public enum ActivityType {
    /**
     * Activity has only one instance per world.
     * MUST have exactly one no-arg constructor.
     */
    SINGLETON_ACTIVITY,
    /**
     * Activity has only one instance per world per location.
     * MUST have exactly one constructor with Location parameter.
     */
    PER_LOCATION_ACTIVITY,
    /**
     * Activity with custom object for each player in this activity.
     * MUST have one no-arg constructor.
     */
    CUSTOM_ACTIVITY
}
