from irc.client import Event, ServerConnection

from timmy import core, db_access, utilities


class PostAuthHandler:
    def __init__(self):
        self.channel_join_started = False

    def on_umode(self, connection: ServerConnection, event: Event) -> None:
        if not self.channel_join_started:
            self.channel_join_started = True
            channels = db_access.channel_db.get_channel_list()

            for channel in channels:
                connection.join(channel)

            core.init_core_tickers()
            utilities.init_utility_tickers()
