import uuid
from typing import Optional

from pubsub import pub

from timmy.communication.irc.bot import Bot


class Irc:
    nickname: str
    realname: str

    host: str
    port: int

    # TODO: Stop storing passwords in memory.
    server_password: Optional[str]

    __bot: Bot

    def __init__(self, settings: dict):
        if 'irc' not in settings:
            raise ValueError('Settings must contain an irc block.')

        if 'sql' not in settings:
            raise ValueError('Settings must contain a sql block')

        if 'nickname' in settings['irc']:
            self.nickname = settings['irc']['nickname']
        else:
            raise ValueError('nickname is a required value')

        if 'realname' in settings['irc']:
            self.realname = settings['irc']['realname']
        else:
            self.realname = settings['irc']['nickname']

        if 'host' in settings['irc']:
            self.host = settings['irc']['host']
        else:
            raise ValueError('host is a required value')

        if 'port' in settings['irc']:
            self.port = int(settings['irc']['port'])
        else:
            self.port = 6667

        if 'server_password' in settings['irc']:
            self.server_password = settings['irc']['server_password']
        else:
            self.server_password = None

        self.__bot = Bot()

    @staticmethod
    def get_default_config():
        return {
            "irc": {
                "nickname": "Timmy",
                "realname": "Timmy",
                "host": "localhost",
                "port": 6667,
                "server_password": None
            },
            "sql": {
                "user": "timmy",
                "password": "password",
                "host": "localhost",
                "port": 3306,
                "database": "timmy"
            }
        }

    def initialize(self, connection_tag: uuid):
        self.__bot.setup(connection_tag, self.nickname, self.realname, self.host, self.port, self.server_password)
        self.__bot.start()

        pub.subscribe(self._join_channel, "join-channel")
        pub.subscribe(self._send_message, "send-message")
        pub.subscribe(self._send_action, "send-action")
        pub.subscribe(self._send_pm, "send-pm")

    def shutdown(self):
        self.__bot.connection.quit()
        return

    def _join_channel(self, message_data: dict):
        return

    def _send_message(self, message_data: dict):
        return

    def _send_action(self, message_data: dict):
        return

    def _send_pm(self, message_data: dict):
        return
