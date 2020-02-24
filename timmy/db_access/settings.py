from timmy import db_access


class Settings:
    def __init__(self):
        self.db = None

    def init(self):
        self.db = db_access.connection_pool

    def get_setting(self, name):
        conn = self.db.get_connection()
        setting_query = "SELECT `value` FROM `settings` WHERE `key` = %(name)s"

        cur = conn.cursor()
        cur.execute(setting_query, {'name': name})

        res = cur.fetchone()

        cur.close()
        conn.close()

        return res[0]
