package dev.rangel.redirectservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
// NOTE: AFTER_CLASS prevents intermittent RedisCommandTimeoutException between test classes.
// Root cause not fully isolated (suspected: Lettuce connection lifecycle vs.
// shared static Testcontainers). Accepted as pragmatism;
// revisit if suite performance cost grows in upcoming phases.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(TestClockConfig.class)
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withInitScript("init-schema.sql");

    @Container
    @ServiceConnection(name = "redis")
    protected static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void clearRedisCache() {
        if (redisConnectionFactory != null) {
            try (RedisConnection connection = redisConnectionFactory.getConnection()) {
                connection.serverCommands().flushAll();
            } catch (Exception e) {
                throw new IllegalStateException("Could not clear Redis cache between integration tests", e);
            }
        }
    }
}