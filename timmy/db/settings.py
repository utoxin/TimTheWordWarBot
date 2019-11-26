from timmy import db


class Settings:
    def __init__(self):
        self.db = db.ConnectionPool.instance()

    def get_setting(self, name):
        conn = self.db.get_connection()
        with conn:
            setting_query = """SELECT value FROM settings WHERE key = ? """
            cur = conn.cursor(prepared=True)
            with cur:
                cur.execute(setting_query, name)
                res = cur.fetchone()
                print(res)
