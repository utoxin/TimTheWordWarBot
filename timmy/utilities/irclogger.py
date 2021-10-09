import inspect
import logging

from timmy import core


class IrcLogger:
    def __init__(self):
        self._enabled = False
        self._log_channel = "#commandcenter"
        self._log_level = logging.INFO

    def set_enabled(self, enabled: bool) -> None:
        self._enabled = enabled

    def set_channel(self, log_channel: str) -> None:
        self._log_channel = log_channel

    def set_log_level(self, log_level: int):
        self._log_level = log_level

    def log_message(self, message, log_level = logging.INFO, layer = 1) -> None:
        if self._enabled and log_level >= self._log_level:
            if isinstance(message, str):
                core.bot_instance.connection.privmsg(self._log_channel, message)
            elif isinstance(message, list):
                for line in message:
                    self.log_message(line, log_level, layer + 1)
            else:
                core.bot_instance.connection.privmsg(
                        self._log_channel,
                        "Received message of type {} from {}".format(
                                type(message),
                                inspect.stack()[layer].function
                        )
                )
