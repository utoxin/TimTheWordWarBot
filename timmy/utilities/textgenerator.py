from pychance import PyChance

from timmy import db_access


class TextGenerator:
    def __init__(self):
        self.pychance = PyChance()

    def load_tables(self):
        db = db_access.connection_pool.get_connection()

        select_query = "SELECT `table_name`, `table_entry` FROM `pychance_basic_tables` `pbt` INNER JOIN " \
                       "`pychance_basic_table_entries` `pbte` ON (`pbt`.`uuid` = `pbte`.`pychance_basic_table_id`)"
        select_cursor = db.cursor(dictionary=True)
        select_cursor.execute(select_query)

        for row in select_cursor:
            if row['table_name'] not in self.pychance.tables:
                table = SimpleTable()
