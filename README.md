# jasync-sql-postgres-connection-refused

TLDR: Executing queries against an offline PostgreSQL always fails the 1st query.

We have implemented an integration test simulating PostgreSQL temporarily unavailable to check how the client handles
this scenario.

We send 100 insert queries, before starting the PostgreSQL container, using the connection pool, and then start the
container.

Our expectation is that 100 queries are successfully executed.

However, only 99 queries succeed, with the 1st query consistently failing with "Connection refused".

This repository provides a simple reproducer of the problem described above.

To reproduce just run the integration test `PostgreSqlUnavailableIT`
