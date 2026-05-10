INSERT INTO users (
    uuid, first_name, last_name, email, password_hash, role, is_active,
    title, summary, phone_number, location, linkedin_url, website_url,
    created_at, updated_at
) VALUES
    (
        '11111111-1111-4111-8111-111111111111',
        'John', 'Doe', 'john.doe@userprofilehub.dev',
        '$2b$12$u9sQRi7bwg0jdW8H7q6rfeSaxOPQmU/Xs4fozPq50hIrt24.FYYjm',
        'ROLE_USER', TRUE,
        'Backend Engineer',
        'Java backend developer focused on secure APIs and distributed systems.',
        '+15551000001',
        'Berlin, DE',
        'https://www.linkedin.com/in/johndoe-dev',
        'https://johndoe.dev',
        NOW(), NOW()
    ),
    (
        '22222222-2222-4222-8222-222222222222',
        'Emma', 'Stone', 'emma.stone@userprofilehub.dev',
        '$2b$12$u9sQRi7bwg0jdW8H7q6rfeSaxOPQmU/Xs4fozPq50hIrt24.FYYjm',
        'ROLE_USER', TRUE,
        'Product Designer',
        'UI/UX designer with experience in enterprise dashboard and mobile design.',
        '+15551000002',
        'Warsaw, PL',
        'https://www.linkedin.com/in/emmadesigns',
        'https://emmastone.design',
        NOW(), NOW()
    ),
    (
        '33333333-3333-4333-8333-333333333333',
        'Noah', 'Khan', 'noah.khan@userprofilehub.dev',
        '$2b$12$u9sQRi7bwg0jdW8H7q6rfeSaxOPQmU/Xs4fozPq50hIrt24.FYYjm',
        'ROLE_USER', TRUE,
        'Data Analyst',
        'Analyst specialized in SQL, BI reporting, and product data storytelling.',
        '+15551000003',
        'Prague, CZ',
        'https://www.linkedin.com/in/noahkhan',
        NULL,
        NOW(), NOW()
    ),
    (
        '44444444-4444-4444-8444-444444444444',
        'Sophia', 'Lee', 'sophia.lee@userprofilehub.dev',
        '$2b$12$u9sQRi7bwg0jdW8H7q6rfeSaxOPQmU/Xs4fozPq50hIrt24.FYYjm',
        'ROLE_USER', TRUE,
        'QA Engineer',
        'Quality engineer focused on automation, API testing, and release quality.',
        '+15551000004',
        'Budapest, HU',
        'https://www.linkedin.com/in/sophialeeqa',
        NULL,
        NOW(), NOW()
    ),
    (
        '55555555-5555-4555-8555-555555555555',
        'Admin', 'User', 'admin@userprofilehub.dev',
        '$2b$12$GOltCxEhDvUP0FVQ4Ej95e5nfPWPRtPbsdqu382GIHqHn196doIse',
        'ROLE_ADMIN', TRUE,
        'System Administrator',
        'Platform administrator responsible for operations and access control.',
        '+15551000005',
        'London, UK',
        'https://www.linkedin.com/in/platformadmin',
        'https://admin.userprofilehub.dev',
        NOW(), NOW()
    )
ON CONFLICT (email) DO NOTHING;
