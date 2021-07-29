-- Fix Tag Structure
-- depends: 20210728_01_hmSy3-fix-text-table-names

UPDATE pychance_basic_table_entries SET table_entry = REGEXP_REPLACE(table_entry, "%\\((.+?)\\)", "[\\1]");