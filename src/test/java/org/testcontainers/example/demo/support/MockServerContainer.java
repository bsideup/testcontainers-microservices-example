package org.testcontainers.example.demo.support;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.client.server.ForwardChainExpectation;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@Slf4j
public class MockServerContainer extends GenericContainer<MockServerContainer> {

    public MockServerContainer() {
        super("jamesdbloom/mockserver:latest");
        withCommand("/opt/mockserver/run_mockserver.sh -logLevel INFO -serverPort 80");
        addExposedPorts(80);

        withLogConsumer(new Slf4jLogConsumer(log));
    }

    @Getter
    private MockServerClient client;

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);

        client = new MockServerClient(getContainerIpAddress(), getMappedPort(getExposedPorts().get(0)));
    }

    public ForwardChainExpectation when(HttpRequest httpRequest, Times times) {
        return client.when(httpRequest, times);
    }

    public ForwardChainExpectation when(HttpRequest httpRequest) {
        return client.when(httpRequest);
    }
}
