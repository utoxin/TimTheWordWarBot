-- Fix Text Table Names
-- depends: 20210725_01_MIv61

UPDATE `pychance_basic_tables` SET `table_name`='greeting' WHERE `table_name`='greetings';
UPDATE `pychance_basic_tables` SET `table_name`='extra_greeting' WHERE `table_name`='extra_greetings';
UPDATE `pychance_basic_tables` SET `table_name`='action' WHERE `table_name`='actions';
UPDATE `pychance_basic_tables` SET `table_name`='challenge_template' WHERE `table_name`='challenge_templates';
