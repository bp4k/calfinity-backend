CREATE TABLE calendars (
    id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    calendar_type VARCHAR(16) NOT NULL
        CHECK (calendar_type IN ('PERSONAL', 'GROUP')),
    color VARCHAR(7) NOT NULL
        CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uq_calendars_active_personal_owner
    ON calendars (owner_id)
    WHERE calendar_type = 'PERSONAL' AND deleted_at IS NULL;

CREATE TABLE calendar_memberships (
    id VARCHAR(36) PRIMARY KEY,
    calendar_id VARCHAR(36) NOT NULL
        REFERENCES calendars (id),
    user_id VARCHAR(255) NOT NULL,
    role VARCHAR(16) NOT NULL
        CHECK (role IN ('EDITOR', 'VIEWER')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_calendar_membership_user
        UNIQUE (calendar_id, user_id)
);
