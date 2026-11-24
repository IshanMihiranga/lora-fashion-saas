-- Spring Modulith event publication registry (transactional outbox, CLAUDE.md §5).
-- Mirrors Spring Modulith 2.2 reference schema v2 for PostgreSQL; mapped by the
-- Modulith JPA entity (table EVENT_PUBLICATION) and validated by Hibernate at start-up.

create table event_publication
(
    id                     uuid                     not null,
    listener_id            text                     not null,
    event_type             text                     not null,
    serialized_event       text                     not null,
    publication_date       timestamp with time zone not null,
    completion_date        timestamp with time zone,
    status                 text,
    completion_attempts    integer                  not null default 0,
    last_resubmission_date timestamp with time zone,
    constraint pk_event_publication primary key (id),
    constraint ck_event_publication_completion_attempts check (completion_attempts >= 0)
);

create index ix_event_publication_serialized_event_hash
    on event_publication using hash (serialized_event);

create index ix_event_publication_completion_date
    on event_publication (completion_date);
