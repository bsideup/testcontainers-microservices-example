package org.testcontainers.example.demo;

import lombok.Value;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Stream;

import static java.lang.Long.parseLong;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@SpringBootApplication
@EnableFeignClients
@RestController
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    GeoClient geoClient;

    @GetMapping("/me/city")
    public GeoClient.GeoResponse getMyCity(@RequestHeader(AUTHORIZATION) String sessionId) {
        val userId = parseLong(redisTemplate.opsForValue().get("sessions/" + sessionId));

        return jdbcTemplate.queryForObject(
                "SELECT latitude, longitude FROM users WHERE id = ?",

                // Hello Tagir :trollface:
                Stream.of(userId).toArray(),

                // REST call in RowMapper? Sure... IT'S A MICRO-SERVICE!!1
                (it, i) -> geoClient.getCity(it.getDouble(1), it.getDouble(2))
        );
    }

    @FeignClient("geo-service")
    interface GeoClient {
        @GetMapping("/cities")
        GeoResponse getCity(
                @RequestParam("latitude") Double latitude,
                @RequestParam("longitude") Double longitude
        );

        @Value // Who needs Kotlin?
        class GeoResponse {
            String city;
        }
    }
}
