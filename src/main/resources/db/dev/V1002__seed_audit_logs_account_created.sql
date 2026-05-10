INSERT INTO audit_logs (
    user_id,
    file_id,
    file_uuid,
    actor_id,
    actor_role,
    action,
    ip_address,
    user_agent,
    acted_at
)
SELECT
    u.id,
    NULL,
    NULL,
    u.id,
    u.role,
    'ACCOUNT_CREATED',
    NULL,
    'DEV_SEED',
    NOW()
FROM users u
WHERE u.email IN (
    'john.doe@userprofilehub.dev',
    'emma.stone@userprofilehub.dev',
    'noah.khan@userprofilehub.dev',
    'sophia.lee@userprofilehub.dev',
    'admin@userprofilehub.dev'
)
AND NOT EXISTS (
    SELECT 1
    FROM audit_logs al
    WHERE al.user_id = u.id
      AND al.actor_id = u.id
      AND al.action = 'ACCOUNT_CREATED'
);
