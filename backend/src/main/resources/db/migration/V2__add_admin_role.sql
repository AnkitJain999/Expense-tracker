-- Adds the ADMIN role, used for user-management endpoints.
ALTER TABLE users
    DROP CONSTRAINT chk_users_role;

ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('EMPLOYEE', 'TEAM_LEAD', 'FINANCE_MANAGER', 'ADMIN'));
