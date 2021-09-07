import random

from irc.client import Event, ServerConnection

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class EightballCommand(BaseCommand):
    user_commands = {'eightball'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if not command_data.in_pm:
            channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

            if not channel_data.command_settings[command_data.command]:
                self.respond_to_user(connection, event, "I'm sorry, I don't do that here.")
                return

        self.eightball_command(connection, event, command_data)

    def eightball_command(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if random.randint(0, 100) < 5:
            from timmy.utilities import markov_generator
            message = markov_generator.generate_markov('say', command_data.arg_string)
        else:
            message = text_generator.get_string('[eightball_answer]')

        self.respond_to_user(connection, event, message)
