package com.berdachuk.medexpertmatch.core.repository.sql;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * BeanPostProcessor that injects SQL queries from external files into fields
 * annotated with {@link InjectSql}.
 *
 * <p>This processor scans all Spring beans for fields annotated with {@code @InjectSql},
 * loads the SQL content from the specified classpath resource, and injects it
 * into the field before bean initialization.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @Repository
 * public class DoctorRepository {
 *     @InjectSql("/sql/doctor/findById.sql")
 *     String findByIdSql;
 *
 *     public Optional<Doctor> findById(String id) {
 *         return namedJdbcTemplate.query(findByIdSql, params, mapper);
 *     }
 * }
 * }
 * </pre>
 *
 * <p>If a SQL file cannot be found, a {@link BeanCreationException} is thrown
 * to fail fast during application startup.
 */
@Slf4j
@Component
public class SqlInjectBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Field[] fields = bean.getClass().getDeclaredFields();
        Arrays.stream(fields)
                .filter(field -> field.isAnnotationPresent(InjectSql.class))
                .forEach(field -> {
                    String filePath = field.getAnnotation(InjectSql.class).value();
                    try {
                        ClassPathResource resource = new ClassPathResource(filePath);
                        if (!resource.exists()) {
                            throw new BeanCreationException(beanName,
                                    "SQL file not found: " + filePath + " for field " + field.getName() + " in " + bean.getClass().getName());
                        }
                        String sql = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);
                        // Trim SQL to remove leading/trailing whitespace and normalize line endings
                        sql = sql.trim().replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
                        field.setAccessible(true);
                        ReflectionUtils.setField(field, bean, sql);
                        log.debug("Injected SQL from {} into field {} of bean {}", filePath, field.getName(), beanName);
                    } catch (IOException e) {
                        throw new BeanCreationException(beanName,
                                "Failed to load SQL file: " + filePath + " for field " + field.getName() + " in " + bean.getClass().getName(), e);
                    }
                });
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
