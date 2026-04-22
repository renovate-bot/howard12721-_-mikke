# syntax=docker/dockerfile:1.23
FROM flyway/flyway:12.4.0

COPY base/database/migrations /flyway/migrations
COPY images/migrations/run-migration.sh /flyway/run-migration.sh

RUN chmod +x /flyway/run-migration.sh

ENTRYPOINT ["/flyway/run-migration.sh"]
