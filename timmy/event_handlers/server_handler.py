import threading

from timmy.utilities import text_generator


class ServerHandler:
    def on_invite(self, connection, event):
        x = threading.Thread(target=self._on_invite, args=(connection, event), name="ServerHandler-on_invite")
        x.start()

    @staticmethod
    def _on_invite(connection, event):
        connection.join(event.arguments[0])

    def on_join(self, connection, event):
        x = threading.Thread(target=self._on_join, args=(connection, event), name="ServerHandler-on_join")
        x.start()

    @staticmethod
    def _on_join(connection, event):
        ch = event.target
        nick = event.source.nick

        if nick != connection.get_nickname():
            text_generator.set_target(nick)
            greeting = text_generator.get_string('[greeting]')
            connection.privmsg(ch, greeting)
