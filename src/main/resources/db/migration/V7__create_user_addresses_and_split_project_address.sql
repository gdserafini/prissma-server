-- Create user_addresses table
CREATE TABLE user_addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    cep VARCHAR(10),
    street VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(2),
    number VARCHAR(20),
    complement VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add columns to construction_projects to replace single address field
ALTER TABLE construction_projects ADD COLUMN cep VARCHAR(10);
ALTER TABLE construction_projects ADD COLUMN street VARCHAR(255);
ALTER TABLE construction_projects ADD COLUMN city VARCHAR(100);
ALTER TABLE construction_projects ADD COLUMN state VARCHAR(2);
ALTER TABLE construction_projects ADD COLUMN number VARCHAR(20);
ALTER TABLE construction_projects ADD COLUMN complement VARCHAR(255);

-- Copy existing address data to street field for migration
UPDATE construction_projects SET street = address WHERE address IS NOT NULL;

-- Drop the old address column (optional - keep if you need backward compatibility)
ALTER TABLE construction_projects DROP COLUMN address;

