-- Add ARQ role to the users table role constraint
ALTER TABLE users DROP CONSTRAINT users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'ENG', 'ARQ', 'USER'));

