package com.tlopes;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.jasync.sql.db.QueryResult;
import com.github.jasync.sql.db.pool.ConnectionPool;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.github.dockerjava.api.model.Ports.Binding.bindPort;
import static org.awaitility.Awaitility.await;

class PostgreSqlUnavailableIT {

    private static final int PORT = 5432;

    private static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:15.2")
            .withInitScript("createDb.sql")
            .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(new HostConfig().withPortBindings(new PortBinding(
                    bindPort(PORT),
                    new ExposedPort(5432)))));
    private static final ConnectionPool<PostgreSQLConnection> connectionPool = PostgreSQLConnectionBuilder.createConnectionPool(
            "jdbc:postgresql://localhost:" + PORT + "/test?user=test&password=test");

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void test() {
        final int insertQueries = 100;
        for (int i = 0; i < insertQueries; i++) {
            final int finalI = i;
            connectionPool.sendQuery("insert into test_insert (value) values('some-value')")
                    .exceptionally(throwable -> {
                        System.out.println("Failed to insert query number " + finalI + ", error: " + throwable.getMessage());
                        return null;
                    });
        }

        System.out.println("Starting postgres");
        postgresql.start();
        System.out.println("Started postgres");
        System.out.println("Futures waiting for connection: " + connectionPool.getFuturesWaitingForConnectionCount());

        try {
            await().atMost(Duration.ofSeconds(5)).until(() -> {
                final QueryResult queryResult = connectionPool.sendQuery("SELECT * FROM test_insert;").get();
                System.out.println("Successful insert queries: " + queryResult.getRows().size() + ", Expected: " + insertQueries);
                return queryResult.getRows().size() == insertQueries;
            });
        } finally {
            postgresql.stop();
        }

    }

}
