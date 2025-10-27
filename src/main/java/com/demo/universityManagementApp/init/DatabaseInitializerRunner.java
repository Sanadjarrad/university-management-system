package com.demo.universityManagementApp.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Spring {@link CommandLineRunner} component for initializing test databases during application startup.
 * This runner executes only when the active Spring profile is "test" and supports multiple dataset initializers.
 * <p>
 * The runner selects the database initializer implementation based on the configured {@code initializerType} property.
 * Supported types include:
 * <ul>
 *     <li>{@code small-dataset} - uses {@link SmallDataSetDatabaseInitializer} to populate a lightweight dataset.</li>
 *     <li>{@code large-dataset} - uses {@link LargeDataSetDatabaseInitializer} to populate a comprehensive dataset.</li>
 * </ul>
 * If an unknown initializer type is provided, the runner logs a warning and skips database initialization.
 * This class is annotated with:
 * {@link Profile}("test") - ensures execution only in the "test" Spring profile.
 * This setup is useful for automated integration testing, allowing test data to be loaded programmatically
 * at application startup without manual intervention.
 */
@Profile("test")
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializerRunner implements CommandLineRunner {

    /**
     * Small dataset initializer implementation.
     * Used when {@link #initializerType} is set to "small-dataset".
     */
    private final SmallDataSetDatabaseInitializer smallDataSetDatabaseInitializer;

    /**
     * Large dataset initializer implementation.
     * Used when {@link #initializerType} is set to "large-dataset".
     */
    private final LargeDataSetDatabaseInitializer largeDataSetDatabaseInitializer;

    /**
     * Determines which dataset initializer to execute.
     * Configurable via {@code app.test.initializer} property. Defaults to {@code small-dataset} if unset.
     */
    @Value("${app.test.initializer:small-dataset}")
    private String initializerType;

    /**
     * Executes the selected database initializer on application startup.
     *
     * @param args the application startup arguments passed to {@link CommandLineRunner#run(String...)}
     * @throws Exception if the underlying database initializer throws any exception during execution
     * Behavior:
     * <ul>
     *     <li>If {@link #initializerType} is "small-dataset", delegates to {@link #smallDataSetDatabaseInitializer}.</li>
     *     <li>If {@link #initializerType} is "large-dataset", delegates to {@link #largeDataSetDatabaseInitializer}.</li>
     *     <li>If {@link #initializerType} does not match any known type, logs a warning and skips initialization.</li>
     * </ul>
     *
     * Logging:
     * <ul>
     *     <li>Info-level log on startup with selected initializer type.</li>
     *     <li>Warn-level log if an unknown initializer type is specified.</li>
     * </ul>
     */
    @Override
    public void run(String... args) throws Exception {
        log.info("InitializerRunner started with type: {}", initializerType);

        switch (initializerType.toLowerCase()) {
            case "small-dataset" -> smallDataSetDatabaseInitializer.run(args);
            case "large-dataset" -> largeDataSetDatabaseInitializer.run(args);
            default -> log.warn("Unknown initializer type '{}'. Skipping database initialization.", initializerType);
        }
    }
}
