-- Tweak DB URLs
-- depends: 20210908_01_NKzvA-add-summon-banish-strings-to-pychance  20210908_01_P2GFZ-add-sing-strings-to-pychance

UPDATE pychance_basic_table_entries t SET t.table_entry = 'GNU Terry Pratchett (https://www.gnuterrypratchett.com/)' WHERE t.table_entry LIKE '%gnuterrypratchett%';
UPDATE pychance_basic_table_entries t SET t.table_entry = 'Have you seen the novels I''ve written previously? You can buy them here: https://amzn.to/3BwoMyx ' WHERE t.table_entry LIKE 'Have you seen the novels%';
UPDATE pychance_basic_table_entries t SET t.table_entry = 'I''m pretty sure I saw the answer to that in my book... https://amzn.to/3BwoMyx' WHERE t.table_entry LIKE 'I''m pretty sure I saw the answer to that in my book%';
UPDATE pychance_basic_table_entries t SET t.table_entry = 'Find the answer in my book! https://amzn.to/3BwoMyx' WHERE t.table_entry LIKE 'Find the answer in my book!%';
