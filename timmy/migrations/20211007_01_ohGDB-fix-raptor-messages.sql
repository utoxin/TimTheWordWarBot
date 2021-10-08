-- Fix Raptor Messages
-- depends: 20211006_02_AWYar-change-out-donation-pool-url

DELETE FROM `pychance_basic_table_entries` WHERE CHAR_LENGTH(`table_entry`) = 1;
DELETE FROM `pychance_basic_tables` WHERE `table_name` LIKE 'raptor_%';