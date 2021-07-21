-- 
-- depends: 20210720_01_XPOPY

ALTER TABLE `users`
    ADD `global_admin` tinyint(1) UNSIGNED NOT NULL DEFAULT 0 AFTER `authed_user`;