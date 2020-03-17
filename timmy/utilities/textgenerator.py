from pychance import PyChance
from pychance.data import SimpleTable

from timmy import db_access


class TextGenerator:
    def __init__(self):
        self.pychance = PyChance()

    def _ensure_init(self):
        if len(self.pychance.tables) < 1:
            self._load_tables()

    def set_target(self, target_string):
        self._ensure_init()
        if 'target' not in self.pychance.tables:
            table = SimpleTable('target')
            self.pychance.add_table('target', table)

        self.pychance.tables['target'].table_values = [target_string]

    def get_string(self, input_string):
        self._ensure_init()
        return self.pychance.parser.parse(input_string)

    def _load_tables(self):
        self.__load_simple_tables()

    def __load_simple_tables(self):
        db = db_access.connection_pool.get_connection()
        select_query = "SELECT `table_name`, `table_entry` FROM `pychance_basic_tables` `pbt` INNER JOIN " \
                       "`pychance_basic_table_entries` `pbte` ON (`pbt`.`uuid` = `pbte`.`pychance_basic_table_id`)"
        select_cursor = db.cursor(dictionary=True)
        select_cursor.execute(select_query)
        for row in select_cursor:
            if row['table_name'] not in self.pychance.tables:
                table = SimpleTable(row['table_name'], [row['table_entry']])
                self.pychance.add_table(row['table_name'], table)
            else:
                self.pychance.tables[row['table_name']].add_value(row['table_entry'])
