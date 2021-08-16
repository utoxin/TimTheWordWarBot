from irc.client import ServerConnection, Event

from timmy.db_access import settings


class AuthHandler:
    def __init__(self):
        self.auth_on_welcome = False
        self.auth_type = ""
        self.auth_data = ""

    def init(self) -> None:
        self.auth_on_welcome = settings.get_setting('auth_on_welcome') == "1"
        self.auth_type = settings.get_setting('auth_type')
        self.auth_data = settings.get_setting('auth_data')

    def on_welcome(self, connection: ServerConnection, event: Event) -> None:
        if self.auth_on_welcome:
            self.handle_auth(connection)

    def handle_auth(self, connection: ServerConnection) -> None:
        if self.auth_type == 'nickserv':
            connection.privmsg('nickserv', 'IDENTIFY ' + self.auth_data)
