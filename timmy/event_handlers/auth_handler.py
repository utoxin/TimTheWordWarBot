from timmy.db_access import settings


class AuthHandler:
    def __init__(self):
        self.auth_on_welcome = settings.get_setting('auth_on_welcome') == "1"
        self.auth_type = settings.get_setting('auth_type')
        self.auth_data = settings.get_setting('auth_data')

    def on_welcome(self, connection, event):
        if self.auth_on_welcome:
            self.handle_auth(connection, event)

    def handle_auth(self, connection, event):
        if self.auth_type == 'nickserv':
            connection.privmsg('nickserv', 'IDENTIFY ' + self.auth_data)
