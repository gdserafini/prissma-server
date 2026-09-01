-- Convites de membro para um workspace. O token nunca é persistido em claro:
-- guarda-se o sha256 hex (indexado -> lookup O(1); bcrypt em loop era o
-- anti-padrão do modelo de referência).

CREATE TABLE member_invites (
    id            BIGSERIAL PRIMARY KEY,
    workspace_id  BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    invited_email VARCHAR(255) NOT NULL,     -- sempre trim + lowercase
    full_name     VARCHAR(150),
    role          VARCHAR(20) NOT NULL DEFAULT 'MEMBER'
                    CHECK (role IN ('ADMIN', 'MEMBER', 'CLIENT')),
    invited_by    BIGINT REFERENCES users(id),
    token_hash    VARCHAR(64) NOT NULL,      -- sha256 hex do token
    expires_at    TIMESTAMPTZ NOT NULL,      -- 7 dias
    accepted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_member_invites_token_hash ON member_invites (token_hash);
CREATE INDEX idx_member_invites_ws_email ON member_invites (workspace_id, invited_email);
