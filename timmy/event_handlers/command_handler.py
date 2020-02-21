import re

from irc.dict import IRCDict

from timmy.data.command_data import CommandData
from timmy.data.command_type import CommandType


class CommandHandler:
    def __init__(self):
        self.timmy_user_command_regex = re.compile('^!(?!skynet)(\\S+)\\s?(.*?)$', re.IGNORECASE)
        self.timmy_admin_command_regex = re.compile('^\\$(?!skynet)(\\S+)\\s?(.*?)$', re.IGNORECASE)

        self.skynet_user_command_regex = re.compile('^!skynet (\\S+)\\s?(.*?)$', re.IGNORECASE)
        self.skynet_admin_command_regex = re.compile('^\\$skynet (\\S+)\\s?(.*?)$', re.IGNORECASE)

        self.user_command_processors = IRCDict()
        self.admin_command_processors = IRCDict()

    def on_pubmsg(self, connection, event):
        command_data = self._parse_event(event)
        if command_data is None:
            return False

        if command_data.type in [CommandType.SKYNET_USER, CommandType.SKYNET_ADMIN]:
            return True

        if command_data.type == CommandType.TIMMY_USER and command_data.command in self.user_command_processors:
            self.user_command_processors[command_data.command].process(command_data)
        elif command_data.type == CommandType.TIMMY_ADMIN and command_data.command in self.admin_command_processors:
            
            self.admin_command_processors[command_data.command].process(command_data)
        else:
            self._unknown_command(connection, event, command_data)

        return True

    def _parse_event(self, event):
        command_data = CommandData()

        timmy_user_match = self.timmy_user_command_regex.match(event.arguments[0])
        if timmy_user_match:
            results = timmy_user_match.groups()
            self._setup_command_data(command_data, event, results)
            command_data.type = CommandType.TIMMY_USER
            return command_data

        timmy_admin_match = self.timmy_admin_command_regex.match(event.arguments[0])
        if timmy_admin_match:
            results = timmy_admin_match.groups()
            self._setup_command_data(command_data, event, results)
            command_data.type = CommandType.TIMMY_ADMIN
            return command_data

        skynet_user_match = self.skynet_user_command_regex.match(event.arguments[0])
        if skynet_user_match:
            results = skynet_user_match.groups()
            self._setup_command_data(command_data, event, results)
            command_data.type = CommandType.SKYNET_USER
            return command_data

        skynet_admin_match = self.skynet_admin_command_regex.match(event.arguments[0])
        if skynet_admin_match:
            results = skynet_admin_match.groups()
            self._setup_command_data(command_data, event, results)
            command_data.type = CommandType.SKYNET_ADMIN
            return command_data

        return None

    @staticmethod
    def _setup_command_data(command_data, event, results):
        command_data.command = results[0]
        command_data.argstring = results[1]
        command_data.args = results[1].split()
        command_data.issuer = event.source.nick

    def _unknown_command(self, connection, event, command_data):
        pass
