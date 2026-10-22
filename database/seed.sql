-- ============================================================================
-- QuickCab seed data (safe sample data only - no users, no passwords).
-- The first admin is created from ADMIN_EMAIL / ADMIN_PASSWORD env vars,
-- and the demo promo codes are seeded by the backend on startup.
-- Run:  psql -U cabuser -d cabbooking -f seed.sql
-- ============================================================================

INSERT INTO promo_codes (code, percent_off, max_discount, active, expires_at)
VALUES
    ('WELCOME50', 50, 100, TRUE, NOW() + INTERVAL '90 days'),
    ('FLAT30',     0,  30, TRUE, NOW() + INTERVAL '90 days'),
    ('MONSOON20', 20,  60, TRUE, NOW() + INTERVAL '30 days')
ON CONFLICT (code) DO NOTHING;
