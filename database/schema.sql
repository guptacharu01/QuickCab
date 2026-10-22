-- ============================================================================
-- QuickCab database schema (PostgreSQL)
-- Canonical schema. For local development the backend can also create these
-- tables automatically via spring.jpa.hibernate.ddl-auto=update.
-- Run:  psql -U cabuser -d cabbooking -f schema.sql
-- ============================================================================

CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    phone         VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,      -- BCrypt hash, never plain text
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('RIDER','DRIVER','ADMIN')),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS drivers (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    vehicle_model       VARCHAR(255) NOT NULL,
    vehicle_plate       VARCHAR(50)  NOT NULL,
    online              BOOLEAN NOT NULL DEFAULT FALSE,
    current_lat         DOUBLE PRECISION,
    current_lng         DOUBLE PRECISION,
    location_updated_at TIMESTAMPTZ,
    rating_avg          DOUBLE PRECISION NOT NULL DEFAULT 0,
    rating_count        INTEGER NOT NULL DEFAULT 0,
    total_earnings      DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS rides (
    id             BIGSERIAL PRIMARY KEY,
    rider_id       BIGINT NOT NULL REFERENCES users(id),
    driver_id      BIGINT REFERENCES users(id),
    pickup_lat     DOUBLE PRECISION NOT NULL,
    pickup_lng     DOUBLE PRECISION NOT NULL,
    dropoff_lat    DOUBLE PRECISION NOT NULL,
    dropoff_lng    DOUBLE PRECISION NOT NULL,
    pickup_label   VARCHAR(500),
    dropoff_label  VARCHAR(500),
    distance_km    DOUBLE PRECISION NOT NULL,
    estimated_fare DOUBLE PRECISION NOT NULL,
    final_fare     DOUBLE PRECISION,
    promo_code     VARCHAR(50),
    status         VARCHAR(30) NOT NULL DEFAULT 'REQUESTED'
                   CHECK (status IN ('REQUESTED','ACCEPTED','DRIVER_ARRIVING',
                                     'IN_PROGRESS','COMPLETED','CANCELLED')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at   TIMESTAMPTZ,
    sos_note       VARCHAR(1000),
    version        BIGINT,                     -- optimistic locking
    CONSTRAINT chk_coords CHECK (
        pickup_lat BETWEEN -90 AND 90 AND dropoff_lat BETWEEN -90 AND 90 AND
        pickup_lng BETWEEN -180 AND 180 AND dropoff_lng BETWEEN -180 AND 180)
);
CREATE INDEX IF NOT EXISTS idx_rides_rider  ON rides(rider_id);
CREATE INDEX IF NOT EXISTS idx_rides_driver ON rides(driver_id);
CREATE INDEX IF NOT EXISTS idx_rides_status ON rides(status);

CREATE TABLE IF NOT EXISTS payments (
    id                      BIGSERIAL PRIMARY KEY,
    ride_id                 BIGINT NOT NULL UNIQUE REFERENCES rides(id) ON DELETE CASCADE,
    amount                  DOUBLE PRECISION NOT NULL CHECK (amount >= 0),
    currency                VARCHAR(10) NOT NULL DEFAULT 'INR',
    stripe_payment_intent_id VARCHAR(255) UNIQUE,  -- Stripe pi_... id, NULL if unpaid
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING','SUCCEEDED','FAILED')),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    paid_at                 TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS ratings (
    id         BIGSERIAL PRIMARY KEY,
    ride_id    BIGINT NOT NULL UNIQUE REFERENCES rides(id) ON DELETE CASCADE,
    driver_id  BIGINT NOT NULL REFERENCES users(id),
    rider_id   BIGINT NOT NULL REFERENCES users(id),
    stars      INTEGER NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment    VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS promo_codes (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(50) NOT NULL UNIQUE,
    percent_off  DOUBLE PRECISION NOT NULL DEFAULT 0,
    max_discount DOUBLE PRECISION NOT NULL DEFAULT 0,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at   TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS support_tickets (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    subject    VARCHAR(255) NOT NULL,
    message    VARCHAR(2000) NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'OPEN'
               CHECK (status IN ('OPEN','IN_PROGRESS','CLOSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_tickets_user ON support_tickets(user_id);
