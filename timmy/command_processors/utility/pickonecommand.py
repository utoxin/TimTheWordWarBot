import random

from irc.client import Event, ServerConnection

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class PickOneCommand(BaseCommand):
    user_commands = {'pickone'}
    interaction_checks = False

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if self._execution_checks(connection, event, command_data):
            if command_data.arg_count == 0:
                self.respond_to_user(connection, event, "Usage: !pickone comma, separated, choices")
                return

            choices = command_data.arg_string.split(',')

            if len(choices) == 1:
                self.respond_to_user(connection, event, "Well, there's not much choice there...")
            else:
                choice = choices[random.randrange(len(choices))]
                self.respond_to_user(connection, event, f"I choose... {choice}")
