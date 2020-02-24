import re

from irc.dict import IRCDict

from timmy.data.command_data import CommandData
from timmy.data.command_type import CommandType


class CommandHandler:
    def __init__(self):
        self.matchers = {
            CommandType.TIMMY_USER:   re.compile('^(!)(?!skynet)(\\S+)\\s?(.*?)$', re.IGNORECASE),
            CommandType.TIMMY_ADMIN:  re.compile('^(\\$)(?!skynet)(\\S+)\\s?(.*?)$', re.IGNORECASE),
            CommandType.SKYNET_USER:  re.compile('^(!skynet )(\\S+)\\s?(.*?)$', re.IGNORECASE),
            CommandType.SKYNET_ADMIN: re.compile('^(\\$skynet )(\\S+)\\s?(.*?)$', re.IGNORECASE)
        }

        self.user_command_processors = IRCDict()
        self.admin_command_processors = IRCDict()

    @staticmethod
    def init_command_processors():
        from timmy import command_processors
        command_processors.register_processors()

    def on_privmsg(self, connection, event):
        self.on_pubmsg(connection, event)

    def on_pubmsg(self, connection, event):
        command_data = self._parse_event(event)
        if command_data is None:
            return False

        if command_data.type in [CommandType.SKYNET_USER, CommandType.SKYNET_ADMIN]:
            return True

        if command_data.type == CommandType.TIMMY_USER and command_data.command in self.user_command_processors:
            self.user_command_processors[command_data.command].process(connection, event, command_data)
        elif command_data.type == CommandType.TIMMY_ADMIN and command_data.command in self.admin_command_processors:
            self.admin_command_processors[command_data.command].process(connection, event, command_data)
        else:
            self._unknown_command(connection, command_data)

        return True

    def _parse_event(self, event):
        command_data = CommandData()

        for command_type, matcher in self.matchers.items():
            match = matcher.match(event.arguments[0])
            if match:
                results = match.groups()
                self._setup_command_data(command_data, event, results)
                command_data.type = command_type
                return command_data

        return None

    @staticmethod
    def _setup_command_data(command_data, event, results):
        command_data.prefix = results[0]
        command_data.command = results[1]
        command_data.args = results[2].split()
        command_data.arg_count = len(command_data.args)
        command_data.arg_string = results[2]
        command_data.issuer = event.source.nick

        if event.type == "privmsg":
            command_data.channel = event.source.nick
        else:
            command_data.channel = event.target

    @staticmethod
    def _unknown_command(connection, command_data):
        connection.privmsg(command_data.channel,
                           command_data.prefix + command_data.command + " was not part of my training.")
