import random

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class SingCommand(BaseCommand):
    user_commands = {'sing'}
    amusement_commands = {'sing'}
    interaction_checks = False

    def process(self, command_data: CommandData) -> None:
        if self._execution_checks(command_data):
            self.sing_command(command_data)

    def sing_command(self, command_data: CommandData) -> None:
        song = text_generator.get_string('[song]')

        random_check = random.randint(0, 100)

        if random_check > 90:
            message = f"sings the well known song '{song}' better than the original artist!"
        elif random_check > 60:
            message = f"chants some obscure lyrics from '{song}'. At least you think that's the name of the song..."
        elif random_check > 30:
            message = f"starts singing '{song}'. You've heard better..."
        else:
            message = f"screeches out some words from '{song}', and all the windows shatter... Ouch."

        self.send_action(command_data, message)
