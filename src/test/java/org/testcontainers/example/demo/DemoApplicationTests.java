package org.testcontainers.example.demo;

import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.util.Pair;
import org.testcontainers.example.demo.DemoApplication.GeoClient.GeoResponse;
import org.testcontainers.example.demo.support.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class DemoApplicationTests extends AbstractIntegrationTest {

    final Pair<Double, Double> expoforum = Pair.of(59.7673985, 30.3570674);

    long userId;

    @Before
    public void setUp() {
        // Prepare DB
        userId = createUser("Sergei Egorov", expoforum);
    }

    @Test
    public void testKnownUser() {
        // Prepare session
        redisTemplate.opsForValue().set("sessions/" + sessionId, userId + "");

        mockServer
                .when(
                        request("/cities")
                                .withQueryStringParameter("latitude", expoforum.getFirst() + "")
                                .withQueryStringParameter("longitude", expoforum.getSecond() + "")
                )
                .respond(
                        response("{\"city\":\"Saint-Petersburg\"}")
                                .withHeader("Content-Type", "application/json")
                );

        val city = restTemplate.getForObject("/me/city", GeoResponse.class);

        assertThat(city)
                .isNotNull()
                .hasFieldOrPropertyWithValue("city", "Saint-Petersburg");
    }

    @Test
    public void testUnknownUser() {
        val entity = restTemplate.getForEntity("/me/city", GeoResponse.class);

        assertThat(entity.getStatusCode().is5xxServerError()).isTrue();
    }
}
