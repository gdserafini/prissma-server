-- RF19: propostas de design e previa visual por IA.
--
-- As tabelas da V4 (design_submissions, design_approvals) nunca sairam do papel:
-- nao existe service, controller nem dado gravado. Esta migration as aproveita em
-- vez de criar um conceito paralelo, porque attachments ja carrega a FK
-- design_submission_id e design_approvals ja modela o parecer de aprovacao.
--
-- O modelo passa a ter dois niveis:
--   design_proposals    -> a proposta, isto e, o card da tela ("Sala de estar")
--   design_submissions  -> a VERSAO da proposta (v1, v2, v3), cada uma com seu
--                          arquivo e seu status de aprovacao
--
-- A V4 amarrava a submissao a uma etapa. A tela lista propostas da OBRA, entao o
-- vinculo sobe para construction_projects e stage_id vira opcional: uma proposta
-- pode, mas nao precisa, estar amarrada a uma etapa do cronograma.

CREATE TABLE design_proposals (
    id BIGSERIAL PRIMARY KEY,
    construction_project_id BIGINT NOT NULL
        REFERENCES construction_projects(id) ON DELETE CASCADE,
    stage_id BIGINT
        REFERENCES stages(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    environment_type VARCHAR(20) NOT NULL
        CHECK (environment_type IN ('LIVING_ROOM', 'BEDROOM', 'KITCHEN', 'BATHROOM',
                                    'DINING_ROOM', 'OFFICE', 'BALCONY', 'GARAGE', 'OTHER')),
    -- created_by_name segue o par assignee_user_id/assignee_name de tasks (V10) e
    -- responsible_user_id/responsible_name do diario (V16): a proposta e historico
    -- e precisa continuar legivel depois que o autor sai do sistema.
    created_by_user_id BIGINT
        REFERENCES users(id) ON DELETE SET NULL,
    created_by_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (construction_project_id, title)
);

-- A listagem e sempre "obra X, da mais recente para a mais antiga, paginada".
-- id DESC entra como desempate para que propostas com o mesmo updated_at nao
-- troquem de pagina entre uma requisicao e outra.
CREATE INDEX idx_design_proposals_project_updated
    ON design_proposals(construction_project_id, updated_at DESC, id DESC);

CREATE INDEX idx_design_proposals_stage_id
    ON design_proposals(stage_id);

CREATE INDEX idx_design_proposals_created_by_user_id
    ON design_proposals(created_by_user_id);

-- design_submissions vira a versao da proposta.
--
-- Nenhuma linha existe, entao as alteracoes dispensam backfill. O CHECK de status
-- fica intacto: DRAFT / PENDING_REVIEW / APPROVED / REJECTED e exatamente o que a
-- tela mostra como Rascunho / Em analise / Aprovada / Ajustes pedidos.

-- Derrubar a coluna leva junto o UNIQUE (stage_id, version) e o indice da V4.
ALTER TABLE design_submissions
    DROP COLUMN stage_id;

-- O titulo passa a ser da proposta: as versoes de "Sala de estar" sao todas da
-- sala de estar, e a tela nomeia o card uma vez so.
ALTER TABLE design_submissions
    DROP COLUMN title;

ALTER TABLE design_submissions
    ADD COLUMN proposal_id BIGINT NOT NULL
        REFERENCES design_proposals(id) ON DELETE CASCADE;

-- version era VARCHAR(50) livre. A tela numera v1/v2/v3 e o servidor calcula o
-- proximo numero, entao o tipo passa a ser inteiro.
ALTER TABLE design_submissions
    ALTER COLUMN version TYPE INTEGER USING version::integer;

-- Uma versao pode nascer sem arquivo: a previa por IA cria a linha antes de a
-- imagem existir, e uma proposta pode ser cadastrada so com titulo e descricao.
ALTER TABLE design_submissions
    ALTER COLUMN file_url DROP NOT NULL;

ALTER TABLE design_submissions
    ALTER COLUMN author_user_id DROP NOT NULL;

ALTER TABLE design_submissions
    DROP CONSTRAINT design_submissions_author_user_id_fkey;

ALTER TABLE design_submissions
    ADD CONSTRAINT design_submissions_author_user_id_fkey
        FOREIGN KEY (author_user_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE design_submissions
    ADD COLUMN author_name VARCHAR(255) NOT NULL DEFAULT '';

ALTER TABLE design_submissions
    ALTER COLUMN author_name DROP DEFAULT;

-- Distingue a versao desenhada a mao da gerada pela IA: a tela marca a segunda
-- com o selo de IA e o historico precisa dizer de onde cada versao veio.
ALTER TABLE design_submissions
    ADD COLUMN generated_by_ai BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE design_submissions
    ADD COLUMN file_name VARCHAR(255);

ALTER TABLE design_submissions
    ADD COLUMN file_type VARCHAR(100);

ALTER TABLE design_submissions
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE design_submissions
    ADD CONSTRAINT design_submissions_proposal_id_version_key
        UNIQUE (proposal_id, version);

CREATE INDEX idx_design_submissions_proposal_version
    ON design_submissions(proposal_id, version DESC);

-- Previa visual por IA.
--
-- A chamada a OpenAI leva de 30 a 60s, longe demais para segurar uma requisicao
-- HTTP: o disparo responde 202 e grava a linha em PROCESSING, um job assincrono
-- conclui e a tela consulta o status por polling. As imagens de entrada (foto do
-- ambiente e planta marcada) NAO viram attachments de proposito -- poluiriam a aba
-- Documentos da obra --, entao ficam no FileStorageService com a chave aqui.
CREATE TABLE ai_environment_previews (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT NOT NULL
        REFERENCES design_proposals(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('PROCESSING', 'READY', 'FAILED')),
    raw_image_key TEXT NOT NULL,
    floor_plan_key TEXT,
    -- Os sete campos de EnvironmentPreviewRequest, guardados como enviados: o
    -- retry reaproveita exatamente as mesmas escolhas do usuario.
    options_json JSONB NOT NULL,
    result_submission_id BIGINT
        REFERENCES design_submissions(id) ON DELETE SET NULL,
    error_message TEXT,
    requested_by_user_id BIGINT
        REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_ai_previews_proposal_created
    ON ai_environment_previews(proposal_id, created_at DESC, id DESC);

CREATE INDEX idx_ai_previews_requested_by_user_id
    ON ai_environment_previews(requested_by_user_id);

-- Nova permissao MANAGE_PROPOSALS: criar proposta, gerar previa e aprovar versao
-- e atribuicao do engenheiro e do arquiteto (a matriz do design esconde o modulo
-- do mestre de obras e da so leitura ao cliente). Aditiva: so amplia a CHECK; obra
-- sem linhas em project_role_permissions continua caindo nos defaults do ProjectRole.
ALTER TABLE project_role_permissions
    DROP CONSTRAINT project_role_permissions_permission_check;

ALTER TABLE project_role_permissions
    ADD CONSTRAINT project_role_permissions_permission_check
    CHECK (permission IN ('VIEW_PROJECT', 'MANAGE_PROJECT', 'MANAGE_MEMBERS',
                          'MANAGE_BUDGET', 'MANAGE_STAGES', 'MANAGE_TEAMS',
                          'MANAGE_TASKS', 'MANAGE_ATTACHMENTS', 'MANAGE_DIARY',
                          'MANAGE_PROPOSALS'));
