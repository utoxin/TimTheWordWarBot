-- 
-- depends: 0001-add-channel-active-column  0002-add-missing-settings

REPLACE INTO
    settings (`key`, `value`)
    VALUES
           ('auth_on_welcome', 1),
           ('auth_type', 'nickserv'),
           ('auth_data', 'password');