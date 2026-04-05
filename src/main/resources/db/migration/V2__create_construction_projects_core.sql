CREATE TABLE construction_projects (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL UNIQUE,
    address VARCHAR(255) NOT NULL,
    project_type VARCHAR(100) NOT NULL,
    category VARCHAR(100) NOT NULL,
    land_area NUMERIC(12,2) NOT NULL CHECK (land_area > 0),
    built_area NUMERIC(12,2) NOT NULL CHECK (built_area > 0),
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNING'
        CHECK (status IN ('PLANNING', 'IN_PROGRESS', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    planned_start_date DATE,
    planned_end_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE construction_project_members (
    id BIGSERIAL PRIMARY KEY,
    construction_project_id BIGINT NOT NULL
        REFERENCES construction_projects(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL
        REFERENCES users(id) ON DELETE CASCADE,
    role_in_project VARCHAR(30) NOT NULL
        CHECK (role_in_project IN ('OWNER', 'ARCHITECT', 'ENGINEER', 'FOREMAN')),
    membership_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (membership_status IN ('ACTIVE', 'INACTIVE')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (construction_project_id, user_id)
);

CREATE TABLE stages (
    id BIGSERIAL PRIMARY KEY,
    construction_project_id BIGINT NOT NULL
        REFERENCES construction_projects(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    display_order INTEGER NOT NULL CHECK (display_order > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED'
        CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'BLOCKED', 'DONE')),
    planned_start_date DATE,
    planned_end_date DATE,
    actual_start_date DATE,
    actual_end_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (construction_project_id, display_order)
);

CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT NOT NULL
        REFERENCES stages(id) ON DELETE CASCADE,
    assignee_user_id BIGINT
        REFERENCES users(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM'
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    status VARCHAR(20) NOT NULL DEFAULT 'TODO'
        CHECK (status IN ('TODO', 'IN_PROGRESS', 'BLOCKED', 'DONE')),
    planned_start_date DATE,
    planned_end_date DATE,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_project_members_project_id
    ON construction_project_members(construction_project_id);

CREATE INDEX idx_project_members_user_id
    ON construction_project_members(user_id);

CREATE INDEX idx_stages_project_id
    ON stages(construction_project_id);

CREATE INDEX idx_tasks_stage_id
    ON tasks(stage_id);

CREATE INDEX idx_tasks_assignee_user_id
    ON tasks(assignee_user_id);
