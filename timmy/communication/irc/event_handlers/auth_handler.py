import threading

from irc.client import Event, ServerConnection
from pubsub import pub

from timmy.communication.irc import thread_local


class AuthHandler:
    def __init__(self):
        self.auth_on_welcome = False
        self.auth_type = ''
        self.auth_data = ''
        self.post_identify = ''
        self.post_auth_sent = False

    def init(self, auth_config: dict):
        self.auth_on_welcome = auth_config['type'] == 'nickserv'
        self.auth_type = auth_config['type']
        self.auth_data = auth_config['data']
        self.post_identify = auth_config['post_identify']
        self.post_auth_sent = False

    def on_welcome(self, connection: ServerConnection, event: Event) -> None:
        if self.auth_on_welcome:
            self.handle_auth(connection)

        pub.sendMessage(
                "server-welcome", message_data = {
                    "connection_tag": thread_local.connection_tag
                }
        )

    def handle_auth(self, connection: ServerConnection) -> None:
        if self.auth_type == 'nickserv':
            connection.privmsg('nickserv', 'IDENTIFY ' + self.auth_data)

    def on_umode(self, connection: ServerConnection, event: Event) -> None:
        if event.target == connection.nickname and not self.post_auth_sent:
            self.post_auth_sent = True
            if self.post_identify != '':
                connection.send_raw(self.post_identify)
