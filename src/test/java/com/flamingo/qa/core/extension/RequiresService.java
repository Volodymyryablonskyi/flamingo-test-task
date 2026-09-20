package com.flamingo.qa.core.extension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which external systems a test class needs. If one is unreachable the class is
 * <em>skipped with a reason</em> rather than failed - these are public demo services, and
 * a red suite caused by someone else's downtime carries no information.
 *
 * <p>{@link Inherited} so a base class can declare it once for every subclass.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@ExtendWith(ServiceHealthExtension.class)
public @interface RequiresService {

    SystemUnderTest[] value();
}
