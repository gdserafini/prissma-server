-- Workspaces (multi-tenancy): a construtora/escritório dona das obras.
-- O workspace decide QUAIS obras o usuário alcança; a obra decide O QUE
-- ele faz lá dentro (construction_project_members, preservado).

CREATE TABLE workspaces (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT NOT NULL REFERENCES users(id),
    name        VARCHAR(130) NOT NULL,
    document    VARCHAR(20),                          -- CNPJ/CPF, opcional
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    is_primary  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

-- Um único workspace primário por dono (entre os não deletados).
CREATE UNIQUE INDEX uq_workspaces_primary_owner
    ON workspaces (owner_id) WHERE is_primary AND deleted_at IS NULL;
CREATE INDEX idx_workspaces_owner_id ON workspaces (owner_id);

CREATE TABLE workspace_members (
    id           BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id      BIGINT NOT NULL REFERENCES users(id),
    role         VARCHAR(20) NOT NULL DEFAULT 'MEMBER'
                   CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'CLIENT')),
    invited_by   BIGINT REFERENCES users(id),
    accepted_at  TIMESTAMPTZ,
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ
);

-- Uma membership viva por (workspace, usuário).
CREATE UNIQUE INDEX uq_workspace_members_ws_user
    ON workspace_members (workspace_id, user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_workspace_members_user
    ON workspace_members (user_id) WHERE is_active;
