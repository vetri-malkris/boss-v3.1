-- Bootstrap platform-level account. Tenant onboarding now requires an authenticated
-- SUPER_ADMIN (see AuthController#register), so one must already exist in the
-- database before anyone can create the first real tenant - there is no public path
-- that could create this account, by design.
INSERT INTO users (id, tenant_id, email, password_hash, full_name, active)
VALUES (
    gen_random_uuid(),
    NULL,
    'superadmin@boss-platform.internal',
    '$2b$10$XLI7P7VWxRFlspZT7xgpaOLeWcBPUAzfAFxr8wVyo0F4d.GjdAEHy',
    'Platform Super Admin',
    TRUE
);

INSERT INTO user_roles (user_id, role)
SELECT id, 'SUPER_ADMIN' FROM users WHERE email = 'superadmin@boss-platform.internal';
