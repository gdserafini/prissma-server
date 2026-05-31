-- RF5: permissões customizadas por papel dentro de cada obra.
-- A ausência de linhas para um (obra, papel) faz o sistema usar as permissões
-- padrão do papel; a presença de linhas sobrescreve completamente esse padrão.
CREATE TABLE project_role_permissions (
    id BIGSERIAL PRIMARY KEY,
    construction_project_id BIGINT NOT NULL
        REFERENCES construction_projects(id) ON DELETE CASCADE,
    role_in_project VARCHAR(30) NOT NULL
        CHECK (role_in_project IN ('OWNER', 'ARCHITECT', 'ENGINEER', 'FOREMAN')),
    permission VARCHAR(40) NOT NULL
        CHECK (permission IN ('VIEW_PROJECT', 'MANAGE_MEMBERS', 'MANAGE_BUDGET',
                              'MANAGE_STAGES', 'MANAGE_TEAMS', 'MANAGE_TASKS',
                              'MANAGE_ATTACHMENTS')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (construction_project_id, role_in_project, permission)
);

CREATE INDEX idx_project_role_permissions_project_role
    ON project_role_permissions(construction_project_id, role_in_project);
