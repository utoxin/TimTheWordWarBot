-- fix color {a} tags
-- depends: 20210831_03_RFEMr-add-dance-strings-to-pychance

UPDATE pychance_basic_table_entries SET table_entry = REGEXP_REPLACE(table_entry, "a\\[acolor]", "{a} [color]");
UPDATE pychance_basic_table_entries SET table_entry = REGEXP_REPLACE(table_entry, "\\[color.2]", "[color]");
