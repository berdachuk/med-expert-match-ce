package com.berdachuk.medexpertmatch.integration;

import com.berdachuk.medexpertmatch.core.config.TestAIConfig;
import com.berdachuk.medexpertmatch.core.config.TestAIConfigListener;
import com.berdachuk.medexpertmatch.core.config.TestProfileExclusions;
import com.berdachuk.medexpertmatch.core.config.TestSecurityConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;

/**
 * Base class for integration tests using Testcontainers.
 * Provides PostgreSQL container with PgVector and Apache AGE extensions.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "spring.ai.openai.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration," +
                        "org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration," +
                        "org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration," +
                        "org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiImageModelAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
                // Set dummy values to prevent auto-configuration from trying to create real models
                "spring.ai.openai.api-key=test-key-dummy",
                "spring.ai.openai.base-url=http://localhost:9999"
        }
)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestAIConfig.class, TestProfileExclusions.class, TestAIConfigListener.class})
@Slf4j
public abstract class BaseIntegrationTest {

    /**
     * Singleton PostgreSQL container shared across all test classes.
     * Using singleton pattern instead of @Testcontainers/@Container to ensure
     * the same container instance is used across all test classes.
     */
    @SuppressWarnings("resource") // Container lifecycle managed manually
    static final PostgreSQLContainer<?> postgres;
    private static final Object INIT_LOCK = new Object();
    private static boolean databaseInitialized = false;

    // Static initializer - starts container ONCE for the entire test suite
    // Container reuse is ENABLED for faster test execution
    // Database schema is recreated from scratch before each test run to ensure clean state
    static {
        @SuppressWarnings("resource") // Container lifecycle managed by shutdown hook and Testcontainers Ryuk
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
                DockerImageName.parse("medexpertmatch-postgres-test:latest")
                        .asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("medexpertmatch_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true) // Reuse container for faster tests
                .withLabel("test", "medexpertmatch-integration")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(120)));

        log.info("Container reuse enabled - database schema will be recreated from scratch for clean state");

        // Start container
        container.start();
        postgres = container;
    }


    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Container is already started in static initializer
        // Just provide the current container URL to Spring
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Enable Flyway to use same migrations as production
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.validate-on-migrate", () -> "true");  // Validation enabled - database schema is recreated from scratch
        registry.add("spring.flyway.schemas", () -> "medexpertmatch");
        registry.add("spring.flyway.default-schema", () -> "medexpertmatch");
        // Note: Database schema is recreated from scratch before each test run, so validation is safe and will catch migration issues
    }

    /**
     * Ensures container is started (should already be started by static initializer).
     */
    private static void startContainerIfNeeded() {
        if (!postgres.isRunning()) {
            postgres.start();
        }
    }

    @BeforeAll
    static void initializeDatabaseOnce() throws Exception {
        synchronized (INIT_LOCK) {
            if (!databaseInitialized) {
                // Ensure container is started
                startContainerIfNeeded();

                // Wait for container to be fully ready and database to accept connections
                waitForDatabaseReady();

                // Initialize database extensions and schema
                initializeDatabase();

                databaseInitialized = true;
            }
        }
    }

    /**
     * Waits for database to be ready to accept connections.
     */
    private static void waitForDatabaseReady() throws Exception {
        int maxRetries = 10;
        long retryDelayMs = 200; // Start with shorter delay
        long maxDelayMs = 2000;
        Exception lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                // Test connection with a simple query
                try (Connection testConn = DriverManager.getConnection(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword())) {

                    // Verify database is ready by executing a simple query
                    try (var stmt = testConn.createStatement()) {
                        stmt.execute("SELECT 1");
                    }

                    // Connection successful and database is ready
                    return;
                }
            } catch (Exception e) {
                lastException = e;
                if (i < maxRetries - 1) { // Don't sleep on the last iteration
                    try {
                        Thread.sleep(retryDelayMs);
                        // Exponential backoff with max delay
                        retryDelayMs = Math.min((long) (retryDelayMs * 1.5), maxDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting for database", ie);
                    }
                }
            }
        }

        throw new RuntimeException(
                "Failed to connect to test database after " + maxRetries + " retries. " +
                        "Container running: " + postgres.isRunning() + ", " +
                        "Container state: " + (postgres.isRunning() ? "running" : "not running") + ", " +
                        "JDBC URL: " + postgres.getJdbcUrl(),
                lastException
        );
    }

    /**
     * Initializes the test database with extensions only.
     * Schema and tables are created by Flyway migrations (same as production).
     * Database schema is recreated from scratch before each test run to ensure clean state.
     */
    private static void initializeDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {

            conn.setAutoCommit(true);

            try (Statement stmt = conn.createStatement()) {
                // Drop and recreate schema to ensure clean state
                // This ensures Flyway migrations run on a fresh schema each time
                try {
                    stmt.execute("DROP SCHEMA IF EXISTS medexpertmatch CASCADE;");
                    log.debug("Dropped existing medexpertmatch schema for clean state");
                } catch (Exception e) {
                    // Schema might not exist, that's fine
                    log.debug("Schema drop failed (might not exist): {}", e.getMessage());
                }

                // Create fresh schema
                stmt.execute("CREATE SCHEMA medexpertmatch;");
                log.debug("Created fresh medexpertmatch schema");

                // Enable vector extension (required for PgVector)
                // Note: V1__initial_schema.sql also creates this, but we do it here
                // to ensure it's available before Flyway runs
                try {
                    stmt.execute("CREATE EXTENSION IF NOT EXISTS vector;");
                } catch (Exception e) {
                    // If extension already exists, that's fine
                    // If there's another error, ignore it - might be a permission issue in some environments
                    String message = e.getMessage();
                    if (message != null && !message.contains("already exists") && !message.contains("extension \"vector\" already exists")) {
                        log.warn("Could not create vector extension: {}", message);
                    }
                }

                // Enable AGE extension (now available in custom test container)
                // Note: V1__initial_schema.sql also creates this
                try {
                    stmt.execute("CREATE EXTENSION IF NOT EXISTS age;");
                } catch (Exception e) {
                    // Extension might already exist, that's fine
                    // Ignore other errors - might be a permission issue in some environments
                }

                // Set up AGE graph (now available in custom test container)
                // Note: V1__initial_schema.sql also does this
                try {
                    // AGE is loaded via shared_preload_libraries in Dockerfile
                    // Set search_path to include ag_catalog for AGE functions
                    stmt.execute("SET search_path = ag_catalog, \"$user\", public, medexpertmatch;");
                    // Drop graph if it exists, then create fresh one
                    try {
                        stmt.execute("SELECT * FROM ag_catalog.drop_graph('medexpertmatch_graph', true);");
                    } catch (Exception e) {
                        // Graph might not exist, that's fine
                    }
                    // create_graph returns void, use PERFORM or SELECT * FROM
                    stmt.execute("SELECT * FROM ag_catalog.create_graph('medexpertmatch_graph');");

                    // Note: AGE cypher functions are available via ag_catalog namespace
                } catch (Exception e) {
                    // Graph creation might fail, log but continue
                    String message = e.getMessage();
                    if (message != null && !message.contains("already exists") && !message.contains("graph already exists")) {
                        log.warn("Could not create AGE graph: {}", message);
                    }
                }

                // Set search path back to medexpertmatch schema
                try {
                    stmt.execute("SET search_path = medexpertmatch, public;");
                } catch (Exception e) {
                    // Ignore - might already be set
                }

                // Note: Tables and indexes are created by Flyway migrations
                // See: src/main/resources/db/migration/V1__initial_schema.sql
                // Flyway will run automatically when Spring Boot application context loads
                // Schema is recreated from scratch before each test run, ensuring clean state
            }
        }
    }

}
