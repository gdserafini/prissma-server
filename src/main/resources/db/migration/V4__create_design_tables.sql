CREATE TABLE design_submissions (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT NOT NULL
        REFERENCES stages(id) ON DELETE CASCADE,
    author_user_id BIGINT NOT NULL
        REFERENCES users(id) ON DELETE RESTRICT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    version VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW'
        CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED')),
    file_url TEXT NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (stage_id, version)
);

CREATE TABLE design_approvals (
    id BIGSERIAL PRIMARY KEY,
    design_submission_id BIGINT NOT NULL
        REFERENCES design_submissions(id) ON DELETE CASCADE,
    approver_user_id BIGINT NOT NULL
        REFERENCES users(id) ON DELETE RESTRICT,
    approval_status VARCHAR(20) NOT NULL
        CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    comment TEXT,
    approved_at TIMESTAMPTZ
);

CREATE INDEX idx_design_submissions_stage_id
    ON design_submissions(stage_id);

CREATE INDEX idx_design_submissions_author_user_id
    ON design_submissions(author_user_id);

CREATE INDEX idx_design_approvals_design_submission_id
    ON design_approvals(design_submission_id);

CREATE INDEX idx_design_approvals_approver_user_id
    ON design_approvals(approver_user_id);
