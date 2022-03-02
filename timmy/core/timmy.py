import threading
import time

from pubsub import pub

from timmy import db_access
from timmy.communication.irc.irc import Irc


class Timmy:
    connection_data: dict[str, dict]
    connection_threads: dict[str, threading.Thread]

    def __init__(self):
        self.connection_data = {}
        self.connection_threads = {}

    def setup(self, host: str, database: str, user: str, password: str, port: int, encrypt_passphrase: str) -> None:
        db_access.init_db_access()

        pool = db_access.connection_pool
        pool.setup(host, database, user, password, encrypt_passphrase, port)

        settings = db_access.settings

        connection_settings = settings.get_connections()

        if len(connection_settings) == 0:
            print("No connection data found. Please configure a connection and try again.")
            exit(1)

        for connection in connection_settings:
            self.connection_data[connection['connection_tag']] = connection

            if connection['module'] == 'irc':
                self.connection_threads[connection['connection_tag']] = Irc(connection)

    def start(self) -> None:
        for tag, connection in self.connection_threads.items():
            connection.start()

        pub.subscribe(self._ready_for_join, "state-ready-for-joins")

        time.sleep(10)

    def _ready_for_join(self, message_data: dict):
        pub.sendMessage('command-join-channel', message_data = {'channel': '#timmy-debug'})
