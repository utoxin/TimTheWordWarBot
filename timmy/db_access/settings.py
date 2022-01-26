import json
from typing import Optional

from timmy import db_access
from timmy.db_access import ConnectionPool


class Settings:
    db: Optional[ConnectionPool]

    def __init__(self):
        self.db = None

    def init(self) -> None:
        self.db = db_access.connection_pool

    def get_setting(self, name: str) -> str:
        conn = self.db.get_connection()
        setting_query = "SELECT `value` FROM `settings` WHERE `key` = %(name)s"

        cur = conn.cursor()
        cur.execute(setting_query, {'name': name})

        res = cur.fetchone()

        cur.close()
        self.db.close_connection(conn)

        return res[0]

    def get_connections(self) -> list[dict]:
        encrypt_passphrase = self.db.get_encryption_key()

        conn = self.db.get_connection()
        connection_query = "SELECT `connection_tag`, `module`, AES_DECRYPT(`config`, UNHEX(SHA2(%(passphrase)s, 512" \
                           "))) AS `config` FROM connections"

        cur = conn.cursor(dictionary=True)
        cur.execute(connection_query, {'passphrase': encrypt_passphrase})

        connections = []

        for connection in cur:
            connection['config'] = json.loads(connection['config'])

            connections.append(connection)

        self.db.close_connection(conn)

        return connections
