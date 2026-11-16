package dev.rangel.analyticsservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    // Singleton container: started once per JVM and never stopped by the JUnit extension,
    // so Spring's cached contexts never point at a dead container.
    @ServiceConnection
    protected static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("linkpulse_analytics_test");

    static {
        postgres.start();
    }
}