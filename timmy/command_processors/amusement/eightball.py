import random

from irc.client import Event, ServerConnection

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class EightballCommand(BaseCommand):
    user_commands = {'eightball'}
    interaction_checks = False

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if self._execution_checks(connection, event, command_data):
            self.eightball_command(connection, event, command_data)

    def eightball_command(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if random.randint(0, 100) < 5:
            from timmy.utilities import markov_generator
            message = markov_generator.generate_markov('say', command_data.arg_string)
        else:
            message = text_generator.get_string('[eightball_answer]')

        self.respond_to_user(connection, event, message)
