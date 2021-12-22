from typing import Optional

from pubsub import pub

from timmy.communication.irc.bot import Bot


class Irc:
    nickname: str
    realname: str

    host: str
    port: int
    server_password: Optional[str]

    __bot: Bot

    def __init__(self, settings: dict):
        if 'nickname' in settings:
            self.nickname = settings['nickname']
        else:
            raise ValueError('nickname is a required value')

        if 'realname' in settings:
            self.realname = settings['realname']
        else:
            self.realname = settings['nickname']

        if 'host' in settings:
            self.host = settings['host']
        else:
            raise ValueError('host is a required value')

        if 'port' in settings:
            self.port = int(settings['port'])
        else:
            self.port = 6667

        if 'server_password' in settings:
            self.server_password = settings['server_password']
        else:
            self.server_password = None

        self.__bot = Bot()

    def initialize(self):
        self.__bot.setup(self.nickname, self.realname, self.host, self.port, self.server_password)
        self.__bot.start()

        pub.subscribe(self._join_channel, "join-channel")

    def _join_channel(self, channel: str):
        return
