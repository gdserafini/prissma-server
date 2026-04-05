CREATE TABLE attachments (
    id BIGSERIAL PRIMARY KEY,
    construction_project_id BIGINT NOT NULL
        REFERENCES construction_projects(id) ON DELETE CASCADE,
    stage_id BIGINT
        REFERENCES stages(id) ON DELETE SET NULL,
    task_id BIGINT
        REFERENCES tasks(id) ON DELETE SET NULL,
    design_submission_id BIGINT
        REFERENCES design_submissions(id) ON DELETE SET NULL,
    uploaded_by_user_id BIGINT
        REFERENCES users(id) ON DELETE SET NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100),
    file_url TEXT NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attachments_project_id
    ON attachments(construction_project_id);

CREATE INDEX idx_attachments_stage_id
    ON attachments(stage_id);

CREATE INDEX idx_attachments_task_id
    ON attachments(task_id);

CREATE INDEX idx_attachments_design_submission_id
    ON attachments(design_submission_id);

CREATE INDEX idx_attachments_uploaded_by_user_id
    ON attachments(uploaded_by_user_id);
