import random

from irc.client import Event, ServerConnection

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class SingCommand(BaseCommand):
    user_commands = {'sing'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if not command_data.in_pm:
            channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

            if not channel_data.command_settings[command_data.command]:
                self.respond_to_user(connection, event, "I'm sorry, I don't do that here.")
                return

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
