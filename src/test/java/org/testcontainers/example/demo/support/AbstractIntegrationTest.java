package org.testcontainers.example.demo.support;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = AbstractIntegrationTest.Initializer.class)
public class AbstractIntegrationTest {

    @ClassRule
    public static GenericContainer redis = new GenericContainer("redis:3.0.6")
            .withExposedPorts(6379);

    @ClassRule
    public static MockServerContainer mockServer = new MockServerContainer();

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            EnvironmentTestUtils.addEnvironment("testcontainers", ctx.getEnvironment(),
                    "spring.redis.host=" + redis.getContainerIpAddress(),
                    "spring.redis.port=" + redis.getMappedPort(6379),

                    "spring.datasource.driverClassName=org.testcontainers.jdbc.ContainerDatabaseDriver",
                    "spring.datasource.url=jdbc:tc:postgresql:///ssu.gov.ua",

                    "geo-service.ribbon.listOfServers=" + mockServer.getContainerIpAddress() + ":" + mockServer.getMappedPort(80)
            );
        }
    }
}
