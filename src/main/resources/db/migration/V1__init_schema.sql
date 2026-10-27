CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_users_email ON users (lower(email));

CREATE TABLE rooms (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    capacity   INT          NOT NULL CHECK (capacity > 0),
    location   VARCHAR(200),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_rooms_name ON rooms (lower(name));

CREATE TABLE bookings (
    id           BIGSERIAL PRIMARY KEY,
    room_id      BIGINT       NOT NULL REFERENCES rooms (id),
    user_id      BIGINT       NOT NULL REFERENCES users (id),
    title        VARCHAR(200) NOT NULL,
    attendees    INT          NOT NULL CHECK (attendees > 0),
    start_time   TIMESTAMPTZ  NOT NULL,
    end_time     TIMESTAMPTZ  NOT NULL,
    status       VARCHAR(20)  NOT NULL CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT ck_bookings_time CHECK (end_time > start_time),
    CONSTRAINT ex_bookings_no_overlap EXCLUDE USING gist (
        room_id WITH =,
        tstzrange(start_time, end_time, '[)') WITH &&
    ) WHERE (status = 'CONFIRMED')
);
CREATE INDEX idx_bookings_room_start ON bookings (room_id, start_time);
CREATE INDEX idx_bookings_user_start ON bookings (user_id, start_time);
