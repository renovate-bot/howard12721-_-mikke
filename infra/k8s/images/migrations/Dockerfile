# syntax=docker/dockerfile:1.25
FROM flyway/flyway:13.7.0

COPY base/database/migrations /flyway/migrations
COPY images/migrations/run-migration.sh /flyway/run-migration.sh

RUN chmod +x /flyway/run-migration.sh

ENTRYPOINT ["/flyway/run-migration.sh"]
