-- Schedule dos integrantes da obra.
--
-- A tela mostra, por obra, uma linha por integrante e uma coluna por dia
-- (semana de segunda a domingo ou mes inteiro), com as horas em que cada um
-- esta alocado. As tarefas do dia NAO sao guardadas aqui: saem de tasks
-- (responsavel + periodo planejado) no momento da consulta.
--
-- schedule_members guarda a responsabilidade do integrante na obra
-- ("Fundacao", "Estrutura", "Instalacoes"). E texto livre digitado na propria
-- tela e existe so para o schedule, por isso nao vira coluna em
-- construction_project_members.
CREATE TABLE schedule_members (
    id BIGSERIAL PRIMARY KEY,
    construction_project_id BIGINT NOT NULL
        REFERENCES construction_projects(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL
        REFERENCES users(id) ON DELETE CASCADE,
    user_responsibility VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (construction_project_id, user_id)
);

CREATE INDEX idx_schedule_members_user_id
    ON schedule_members(user_id);

-- Uma linha por integrante e dia com horas alocadas. Dia livre e ausencia de
-- linha, entao hours e sempre positivo.
CREATE TABLE schedule_allocations (
    id BIGSERIAL PRIMARY KEY,
    construction_project_id BIGINT NOT NULL
        REFERENCES construction_projects(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL
        REFERENCES users(id) ON DELETE CASCADE,
    allocation_date DATE NOT NULL,
    hours NUMERIC(4,2) NOT NULL CHECK (hours > 0 AND hours <= 24),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (construction_project_id, user_id, allocation_date)
);

-- A consulta e sempre "obra X, entre as datas A e B".
CREATE INDEX idx_schedule_allocations_project_date
    ON schedule_allocations(construction_project_id, allocation_date);

CREATE INDEX idx_schedule_allocations_user_id
    ON schedule_allocations(user_id);
