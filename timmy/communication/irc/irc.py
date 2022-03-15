import threading
from typing import Optional

from pubsub import pub

from timmy.communication.irc import thread_local
from timmy.communication.irc.bot import Bot


class Irc(threading.Thread):
    connection_tag: str

    nickname: str
    realname: str

    host: str
    port: int

    server_password: Optional[str]

    auth_config: dict

    bot: Bot = Bot()
    __connection_state: str

    def __init__(self, settings: dict):
        thread_name = f"{settings['module']}-{settings['connection_tag']}"

        super().__init__(group=None, target=None, name=thread_name, daemon=False)

        self.connection_tag = settings['connection_tag']

        if 'irc' not in settings['config']:
            raise ValueError('Settings must contain an irc block.')

        if 'sql' not in settings['config']:
            raise ValueError('Settings must contain a sql block')

        if 'nickname' in settings['config']['irc']:
            self.nickname = settings['config']['irc']['nickname']
        else:
            raise ValueError('nickname is a required value')

        if 'realname' in settings['config']['irc']:
            self.realname = settings['config']['irc']['realname']
        else:
            self.realname = settings['config']['irc']['nickname']

        if 'host' in settings['config']['irc']:
            self.host = settings['config']['irc']['host']
        else:
            raise ValueError('irc host is a required value')

        if 'port' in settings['config']['irc']:
            self.port = int(settings['config']['irc']['port'])
        else:
            self.port = 6667

        if 'server_password' in settings['config']['irc']:
            self.server_password = settings['config']['irc']['server_password']
        else:
            self.server_password = None

        if 'user_auth' in settings['config']['irc']:
            self.auth_config = settings['config']['irc']['user_auth']
        else:
            self.auth_config = {
                'type': '',
                'data': '',
                'post_identify': ''
            }

        self.__connection_state = 'INIT'

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

    def run(self):
        # pub.subscribe(self._join_channel, "join-channel")
        # pub.subscribe(self._shutdown, "shutdown")

        thread_local.connection_tag = self.connection_tag

        pub.subscribe(self._server_welcome, "server-welcome")

        self.__connection_state = "CONNECTING"
        self.bot.setup(self.connection_tag, self.nickname, self.realname, self.host, self.port, self.server_password,
                       self.auth_config)
        self.bot.start()

    def _shutdown(self):
        self.bot.connection.quit()
        return

    def _join_channel(self, message_data: dict):
        self.bot.connection.join(message_data["channel"])
        return

    def _send_message(self, message_data: dict):
        return

    def _send_action(self, message_data: dict):
        return

    def _send_pm(self, message_data: dict):
        return

    def _server_welcome(self, message_data: dict) -> None:
        if message_data['connection_tag'] == self.connection_tag:
            if self.__connection_state == 'CONNECTING':
                pub.unsubscribe(self._server_welcome, "server-welcome")

                if self.server_password is not None:
                    self.__connection_state = 'JOINING'
                    pub.subscribe(self._join_channel, "command-join-channel")
                    pub.sendMessage(
                            "state-ready-for-joins", message_data = {
                                "connection_tag": self.connection_tag
                            }
                    )

                else:
                    self.__connection_state = 'CONNECTED'

                    pub.subscribe(self._authenticated, "authenticated")

    def _authenticated(self, message_data: dict) -> None:
        if message_data['connection_tag'] == self.connection_tag:
            if self.__connection_state == 'CONNECTED':
                self.__connection_state = 'JOINING'
                pub.unsubscribe(self._authenticated, "authenticated")

                pub.subscribe(self._join_channel, "command-join-channel")
                pub.sendMessage(
                        "state-ready-for-joins", message_data = {
                            "connection_tag": self.connection_tag
                        }
                )

                pub.subscribe(self._send_message, "send-message")
                pub.subscribe(self._send_action, "send-action")
                pub.subscribe(self._send_pm, "send-pm")
        return
