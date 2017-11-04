package org.testcontainers.example.demo.support;

import lombok.val;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;

import java.util.UUID;
import java.util.stream.Stream;

import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static java.util.Collections.singletonList;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.driverClassName=org.testcontainers.jdbc.ContainerDatabaseDriver",
                "spring.datasource.url=jdbc:tc:postgresql:///mil.ru",
        }
)
public abstract class AbstractIntegrationTest {

    public static GenericContainer redis = new GenericContainer("redis:3.0.6")
            .withExposedPorts(6379);

    public static MockServerContainer mockServer = new MockServerContainer();

    static {
        Stream.of(redis, mockServer).parallel().forEach(GenericContainer::start);

        System.setProperty("spring.redis.host", redis.getContainerIpAddress());
        System.setProperty("spring.redis.port", redis.getFirstMappedPort() + "");

        System.setProperty("geo-service.ribbon.listOfServers", mockServer.getContainerIpAddress() + ":" + mockServer.getFirstMappedPort());
    }

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected String sessionId = UUID.randomUUID().toString();

    @Before
    public void setUpRestTemplate() {
        // Prepare REST template
        restTemplate.getRestTemplate().setInterceptors(singletonList(
                (request, body, execution) -> {
                    request.getHeaders().add(AUTHORIZATION, sessionId);
                    return execution.execute(request, body);
                }
        ));
    }

    protected Long createUser(String name, Pair<Double, Double> coordinates) {
        val holder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                it -> {
                    val stmt = it.prepareStatement(
                            "INSERT INTO users (name, latitude, longitude) VALUES (?, ?, ?) RETURNING id",
                            RETURN_GENERATED_KEYS
                    );
                    stmt.setString(1, name);
                    stmt.setDouble(2, coordinates.getFirst());
                    stmt.setDouble(3, coordinates.getSecond());
                    return stmt;
                },
                holder
        );
        return holder.getKey().longValue();
    }
}
