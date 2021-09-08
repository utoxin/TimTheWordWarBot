-- fix item.2 tags
-- depends: 20210831_04_S9Ze0-fix-color-a-tags

UPDATE pychance_basic_table_entries
SET table_entry = REGEXP_REPLACE(table_entry, "\\[item.2]", "[item]");
