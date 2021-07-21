--
-- file: 0002-migrations/add-missing-settings.sql
--

CREATE UNIQUE INDEX `settings_key_uindex`
	ON `settings` (`key`);

REPLACE INTO
    settings (`key`, `value`)
    VALUES
           ('nickname', 'Timmy'),
           ('realname', 'Timmy'),
           ('server', 'timmy-irc'),
           ('do_irc_logging', 1),
           ('irc_logging_channel', '#timmy-debug');