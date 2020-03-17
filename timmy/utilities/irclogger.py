import inspect

from timmy import core


class IrcLogger:
    def __init__(self):
        self.enabled = False
        self.log_channel = "#commandcenter"

    def set_enabled(self, enabled):
        self.enabled = enabled

    def set_channel(self, log_channel):
        self.log_channel = log_channel

    def log_message(self, message, layer=1):
        if self.enabled:
            if isinstance(message, str):
                core.bot_instance.connection.privmsg(self.log_channel, message)
            elif isinstance(message, list):
                for line in message:
                    self.log_message(line, layer + 1)
            else:
                core.bot_instance.connection.privmsg(self.log_channel,
                                                     "Received message of type {} from {}".format(
                                                             type(message),
                                                             inspect.stack()[layer].function
                                                     ))
