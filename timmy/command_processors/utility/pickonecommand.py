import random

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class PickOneCommand(BaseCommand):
    user_commands = {'pickone'}
    interaction_checks = False

    def process(self, command_data: CommandData) -> None:
        if self._execution_checks(command_data):
            if command_data.arg_count == 0:
                self.respond_to_user(command_data, "Usage: !pickone comma, separated, choices")
                return

            choices = command_data.arg_string.split(',')

            if len(choices) == 1:
                self.respond_to_user(event, "Well, there's not much choice there...")
            else:
                choice = choices[random.randrange(len(choices))]
                self.respond_to_user(command_data, f"I choose... {choice}")
