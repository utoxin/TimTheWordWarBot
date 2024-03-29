import random

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class DanceCommand(BaseCommand):
    user_commands = ['dance']
    amusement_commands = ['dance']
    interaction_checks = False

    def process(self, command_data: CommandData) -> None:
        if self._execution_checks(command_data):
            self.dance_command(command_data)

    def dance_command(self, command_data: CommandData) -> None:
        dance = text_generator.get_string('[dance]')

        random_check = random.randint(0, 100)

        if random_check > 90:
            message = f"dances the {dance} so well he should be on Dancing with the Stars!"
        elif random_check > 60:
            message = f"does the {dance}, and tears up the dance floor."
        elif random_check > 30:
            message = f"attempts to do the {dance}, but obviously needs more practice."
        else:
            message = f"flails about in a fashion that vaguely resembles the {dance}. Sort of."

        self.send_action(command_data, message)
