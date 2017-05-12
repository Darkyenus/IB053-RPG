package ib053.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Annotation that MUST be present on all activities that are to be assigned to players. */
@Retention(RetentionPolicy.RUNTIME)
public @interface Activity {
    /** What is the type of this activity? */
    ActivityType value();
}
