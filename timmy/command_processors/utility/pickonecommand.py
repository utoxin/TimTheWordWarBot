import random

from irc.client import Event

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class PickOneCommand(BaseCommand):
    user_commands = {'pickone'}
    interaction_checks = False

    def process(self, event: Event, command_data: CommandData) -> None:
        if self._execution_checks(event, command_data):
            if command_data.arg_count == 0:
                self.respond_to_user(event, "Usage: !pickone comma, separated, choices")
                return

            choices = command_data.arg_string.split(',')

            if len(choices) == 1:
                self.respond_to_user(event, "Well, there's not much choice there...")
            else:
                choice = choices[random.randrange(len(choices))]
                self.respond_to_user(event, f"I choose... {choice}")
