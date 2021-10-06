import threading

from irc.client import Event, ServerConnection

from timmy import core, db_access, utilities


class PostAuthHandler:
    def __init__(self):
        self.channel_join_started = False

    def on_umode(self, connection: ServerConnection, event: Event) -> None:
        if not self.channel_join_started:
            self.channel_join_started = True
            channels = db_access.channel_db.get_channel_list()

            delay = 0
            delay_step = 0.2

            for channel in channels:
                delay += delay_step
                y = threading.Timer(delay, self._timer_thread, args = [channel])
                y.start()

            y = threading.Timer(delay + delay_step, self._timer_thread_two)
            y.start()

    @staticmethod
    def _timer_thread(channel: str) -> None:
        from timmy.core import bot_instance
        bot_instance.connection.join(channel)

    @staticmethod
    def _timer_thread_two() -> None:
        core.init_core_tickers()
        utilities.init_utility_tickers()
