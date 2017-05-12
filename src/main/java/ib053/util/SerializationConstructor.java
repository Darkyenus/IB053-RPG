package ib053.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Documentation annotation for constructors intended only for serialization
 */
@Retention(RetentionPolicy.SOURCE)
public @interface SerializationConstructor {
}
