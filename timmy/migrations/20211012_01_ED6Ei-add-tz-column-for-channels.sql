-- Add TZ Column For Channels
-- depends: 20211008_01_MHssv-i-am-groot

ALTER TABLE `channels` ADD COLUMN `timezone` VARCHAR(255) DEFAULT 'UTC' AFTER `active`;