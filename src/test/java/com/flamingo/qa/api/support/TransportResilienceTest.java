package com.flamingo.qa.api.support;

import com.flamingo.qa.api.spec.RequestSpecs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the two transport guarantees the rest of the suite leans on: the configured socket
 * timeout is in force, and a transport failure is really retried.
 *
 * <p>Both were written after one request to a stalled Heroku dyno hung for six minutes. The
 * investigation found two defects invisible by reading the code - a read timeout arrives as
 * a <em>checked</em> {@link SocketTimeoutException}, which {@code catch (RuntimeException)}
 * lets past; and {@code FilterContext.next()} is single-use, so a retry filter answers the
 * second call with {@code null}.
 *
 * <p>The fake server accepts and never replies, which is precisely what a <em>connect</em>
 * timeout does not catch. Counting accepted connections counts attempts.
 *
 * <p>{@link Isolated} because it overrides system properties, which are global.
 */
@Tag("unit")
@Isolated
@DisplayName("Transport resilience")
class TransportResilienceTest {

    private static final Duration SOCKET_TIMEOUT = Duration.ofMillis(800);

    private final ExecutorService serverThreads = Executors.newCachedThreadPool();
    private final AtomicInteger acceptedConnections = new AtomicInteger();
    private final List<Socket> heldConnections = new ArrayList<>();

    private ServerSocket server;

    @AfterEach
    void tearDown() throws IOException {
        serverThreads.shutdownNow();
        synchronized (heldConnections) {
            for (Socket socket : heldConnections) {
                socket.close();
            }
        }
        if (server != null) {
            server.close();
        }
        System.clearProperty("api.base.url");
        System.clearProperty("api.timeout.ms");
        System.clearProperty("api.retry.max.attempts");
        System.clearProperty("api.retry.backoff.ms");
    }

    @Test
    @DisplayName("gives up on a server that accepts the connection and never answers")
    void abortsWhenTheServerNeverResponds() throws IOException {
        startSilentServer();
        configure(1);

        long startedAt = System.nanoTime();
        assertThatThrownBy(TransportResilienceTest::ping)
                .as("a stalled server must surface as a failure, not as a hang")
                .hasRootCauseInstanceOf(SocketTimeoutException.class);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        assertThat(elapsed)
                .as("gave up after %s, so the configured socket timeout is in force", elapsed)
                .isLessThan(SOCKET_TIMEOUT.multipliedBy(6));
        assertThat(acceptedConnections).hasValue(1);
    }

    @Test
    @DisplayName("retries a timed-out request the configured number of times")
    void retriesTransportFailures() throws IOException {
        startSilentServer();
        int maxAttempts = 3;
        configure(maxAttempts);

        assertThatThrownBy(TransportResilienceTest::ping)
                .hasRootCauseInstanceOf(SocketTimeoutException.class);

        assertThat(acceptedConnections)
                .as("one connection per attempt")
                .hasValue(maxAttempts);
    }

    private static void ping() {
        TransientFailureRetry.send(() -> given().spec(RequestSpecs.unauthenticated()).when().get("/ping"));
    }

    private void configure(int maxAttempts) {
        System.setProperty("api.base.url", "http://localhost:" + server.getLocalPort());
        System.setProperty("api.timeout.ms", String.valueOf(SOCKET_TIMEOUT.toMillis()));
        System.setProperty("api.retry.max.attempts", String.valueOf(maxAttempts));
        System.setProperty("api.retry.backoff.ms", "50");
    }

    /** Accepts every connection, holds it open and never writes a byte. */
    private void startSilentServer() throws IOException {
        server = new ServerSocket(0);
        serverThreads.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = server.accept();
                synchronized (heldConnections) {
                    heldConnections.add(socket);
                }
                acceptedConnections.incrementAndGet();
            }
            return null;
        });
    }
}
