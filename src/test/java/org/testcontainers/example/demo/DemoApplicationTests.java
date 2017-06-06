package org.testcontainers.example.demo;

import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.testcontainers.example.demo.DemoApplication.GeoClient.GeoResponse;
import org.testcontainers.example.demo.support.AbstractIntegrationTest;

import java.util.UUID;

import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class DemoApplicationTests extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    TestRestTemplate restTemplate;

    final Pair<Double, Double> kiev = Pair.of(50.4016991, 30.2525137);

    String sessionId = UUID.randomUUID().toString();

    long userId;

    @Before
    public void setUp() throws Exception {
        // Prepare DB
        val holder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                it -> {
                    val stmt = it.prepareStatement(
                            "INSERT INTO users (name, latitude, longitude) VALUES (?, ?, ?) RETURNING id",
                            RETURN_GENERATED_KEYS
                    );
                    stmt.setString(1, "Sergei Egorov");
                    stmt.setDouble(2, kiev.getFirst());
                    stmt.setDouble(3, kiev.getSecond());
                    return stmt;
                },
                holder
        );
        userId = holder.getKey().longValue();

        // Prepare session
        redisTemplate.opsForValue().set("sessions/" + sessionId, userId + "");

        // Prepare REST template
        restTemplate.getRestTemplate().setInterceptors(singletonList(
                (request, body, execution) -> {
                    request.getHeaders().add(AUTHORIZATION, sessionId);
                    return execution.execute(request, body);
                }
        ));
    }

    @Test
    public void testKnownUser() {
        mockServer
                .when(
                        request("/cities")
                                .withQueryStringParameter("latitude", kiev.getFirst() + "")
                                .withQueryStringParameter("longitude", kiev.getSecond() + "")
                )
                .respond(
                        response("{\"city\":\"Kiev\"}")
                                .withHeader("Content-Type", "application/json")
                );

        val city = restTemplate.getForObject("/me/city", GeoResponse.class);

        assertThat(city)
                .isNotNull()
                .hasFieldOrPropertyWithValue("city", "Kiev");
    }

    @Test
    public void testUnknownUser() {
        redisTemplate.delete("sessions/" + sessionId);

        val entity = restTemplate.getForEntity("/me/city", GeoResponse.class);

        assertThat(entity.getStatusCode().is5xxServerError()).isTrue();
    }
}
