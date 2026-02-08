package com.berdachuk.medexpertmatch.core.repository.sql;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marker annotation for SqlInjectBeanPostProcessor.
 * Used to inject SQL queries from external files into repository fields.
 * <p>
 * Example:
 * <pre>
 * {@code
 * @InjectSql("/sql/doctor/findById.sql")
 * String findByIdSql;
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectSql {
    /**
     * Path to the SQL file in the classpath.
     * Should start with "/" and be relative to src/main/resources.
     *
     * @return The classpath path to the SQL file
     */
    String value();
}
