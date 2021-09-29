from irc.client import Event, ServerConnection

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class WootCommand(BaseCommand):
    user_commands = {'woot'}
    interaction_checks = False

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if self._execution_checks(connection, event, command_data):
            self.send_action(connection, event, "cheers! Hooray!")
