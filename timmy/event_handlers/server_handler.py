import threading


class ServerHandler:
    def on_invite(self, connection, event):
        x = threading.Thread(target=self._on_invite, args=(connection, event))
        x.start()

    def _on_invite(self, connection, event):
        connection.join(event.arguments[[0]])

