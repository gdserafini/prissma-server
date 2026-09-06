-- RF17: diario da obra.
--
-- Cada linha e um registro pontual do que aconteceu na obra: data com dia e
-- horario, tipo (ocorrencia, entrega, efetivo ou impedimento), responsavel,
-- uma breve descricao e, opcionalmente, um anexo ja carregado na obra.
--
-- responsible_user_id e ON DELETE SET NULL e responsible_name guarda o nome no
-- momento do registro, seguindo o mesmo par assignee_user_id/assignee_name de
-- tasks (V10): o diario e um historico e precisa continuar legivel depois que
-- o usuario sai do sistema.
CREATE TABLE construction_diary_entries (
    id BIGSERIAL PRIMARY KEY,
    construction_project_id BIGINT NOT NULL
        REFERENCES construction_projects(id) ON DELETE CASCADE,
    entry_date TIMESTAMPTZ NOT NULL,
    entry_type VARCHAR(20) NOT NULL
        CHECK (entry_type IN ('OCCURRENCE', 'DELIVERY', 'WORKFORCE', 'IMPEDIMENT')),
    responsible_user_id BIGINT
        REFERENCES users(id) ON DELETE SET NULL,
    responsible_name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    attachment_id BIGINT
        REFERENCES attachments(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- A listagem e sempre "obra X, do mais recente para o mais antigo, paginada".
-- id DESC entra no indice como desempate para que registros com a mesma
-- entry_date nao troquem de pagina entre uma requisicao e outra.
CREATE INDEX idx_diary_entries_project_date
    ON construction_diary_entries(construction_project_id, entry_date DESC, id DESC);

CREATE INDEX idx_diary_entries_responsible_user_id
    ON construction_diary_entries(responsible_user_id);

CREATE INDEX idx_diary_entries_attachment_id
    ON construction_diary_entries(attachment_id);

-- Nova permissao MANAGE_DIARY: escrever no diario e uma atribuicao propria
-- (RF17 aponta o mestre de obras), nao coberta por MANAGE_STAGES ou
-- MANAGE_TASKS. Aditiva: so amplia a CHECK constraint; obra sem linhas em
-- project_role_permissions continua caindo nos defaults do ProjectRole.
ALTER TABLE project_role_permissions
    DROP CONSTRAINT project_role_permissions_permission_check;

ALTER TABLE project_role_permissions
    ADD CONSTRAINT project_role_permissions_permission_check
    CHECK (permission IN ('VIEW_PROJECT', 'MANAGE_PROJECT', 'MANAGE_MEMBERS',
                          'MANAGE_BUDGET', 'MANAGE_STAGES', 'MANAGE_TEAMS',
                          'MANAGE_TASKS', 'MANAGE_ATTACHMENTS', 'MANAGE_DIARY'));
