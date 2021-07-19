--
-- file: migrations/0001-add-channel-active-column.sql
--

ALTER TABLE `channels`
    ADD `active` tinyint(1) UNSIGNED NOT NULL DEFAULT 1 AFTER `channel`;