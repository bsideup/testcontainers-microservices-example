package org.testcontainers.example.demo.support;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.client.server.ForwardChainExpectation;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;

@Slf4j
public class MockServerContainer extends GenericContainer<MockServerContainer> {

    public MockServerContainer() {
        super("jamesdbloom/mockserver:latest");
        withCommand("/opt/mockserver/run_mockserver.sh -logLevel INFO -serverPort 80");
        addExposedPorts(80);

        withLogConsumer(outputFrame -> {
            if (outputFrame != null) {
                String utf8String = outputFrame.getUtf8String();

                if (utf8String != null) {
                    OutputFrame.OutputType outputType = outputFrame.getType();
                    String message = utf8String.trim();

                    switch (outputType) {
                        case END:
                            break;
                        case STDOUT:
                        case STDERR:
                            System.out.println(message);
                            break;
                        default:
                            throw new IllegalArgumentException("Unexpected outputType " + outputType);
                    }
                }
            }
        });
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
