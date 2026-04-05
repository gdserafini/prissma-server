CREATE TABLE project_budgets (
    id BIGSERIAL PRIMARY KEY,
    construction_project_id BIGINT NOT NULL UNIQUE
        REFERENCES construction_projects(id) ON DELETE CASCADE,
    description TEXT,
    planned_total NUMERIC(14,2) NOT NULL CHECK (planned_total >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE budget_items (
    id BIGSERIAL PRIMARY KEY,
    project_budget_id BIGINT NOT NULL
        REFERENCES project_budgets(id) ON DELETE CASCADE,
    category VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    planned_amount NUMERIC(14,2) NOT NULL CHECK (planned_amount >= 0)
);

CREATE TABLE expenses (
    id BIGSERIAL PRIMARY KEY,
    budget_item_id BIGINT NOT NULL
        REFERENCES budget_items(id) ON DELETE CASCADE,
    stage_id BIGINT
        REFERENCES stages(id) ON DELETE SET NULL,
    description TEXT NOT NULL,
    amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    supplier VARCHAR(255),
    receipt_url TEXT,
    spent_at DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_budget_items_budget_id
    ON budget_items(project_budget_id);

CREATE INDEX idx_expenses_budget_item_id
    ON expenses(budget_item_id);

CREATE INDEX idx_expenses_stage_id
    ON expenses(stage_id);
