from timmy import db_access


class PostAuthHandler:
    def __init__(self):
        self.channel_join_started = False

    def on_umode(self, connection, event):
        if not self.channel_join_started:
            self.channel_join_started = True
            channels = db_access.channel_db.get_channel_list()

            for channel in channels:
                connection.join(channel)
