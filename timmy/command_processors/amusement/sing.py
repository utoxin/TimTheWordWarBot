import random

from irc.client import Event, ServerConnection

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class SingCommand(BaseCommand):
    user_commands = {'sing'}
    interaction_checks = False

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if self._execution_checks(connection, event, command_data):
            self.sing_command(connection, event)

    def sing_command(self, connection: ServerConnection, event: Event) -> None:
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

        self.send_action(connection, event, message)
