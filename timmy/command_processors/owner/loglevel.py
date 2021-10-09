import logging

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class LogLevelCommand(BaseCommand):
    admin_commands = ['loglevel']

    __loglevel_map = {
        'debug': logging.DEBUG,
        'info': logging.INFO,
        'warning': logging.WARNING,
        'error': logging.ERROR,
        'critical': logging.CRITICAL
    }

    help_topics = [('admin', 'owner commands', '$loglevel <level>', 'Set the active log level.')]

    def process(self, command_data: CommandData):
        from timmy.utilities import irc_logger

        if command_data.issuer_data.global_admin:
            if command_data.arg_count != 1 or command_data.args[0].lower() not in self.__loglevel_map:
                self.respond_to_user(command_data, "Usage: $loglevel <debug/info/warning/error/critical>")
            else:
                irc_logger.set_log_level(self.__loglevel_map.get(command_data.args[0].lower(), logging.INFO))
                self.respond_to_user(command_data, "Log level set.")
        else:
            self.respond_to_user(command_data, "Only the bot owner can do that!")
