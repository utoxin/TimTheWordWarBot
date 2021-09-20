import random

from irc.client import Event, ServerConnection

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData


class ExpoundCommand(BaseCommand):
    user_commands = {'expound'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if not command_data.in_pm:
            channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

            if not channel_data.command_settings['expound']:
                self.respond_to_user(connection, event, "I'm sorry, I don't do that here.")
                return
        else:
            self.respond_to_user(connection, event, "Sorry, this command doesn't currently work in private messages.")
            return

        from timmy.utilities import markov_generator

        choice = random.randrange(0, 100)
        message_type = 'say'

        if choice < 5 or (choice < 10 and command_data.arg_count > 0):
            message_type = 'mutter'
        elif choice < 20:
            message_type = 'emote'
        elif choice < 40:
            message_type = 'novel'

        markov_generator.random_action(channel_data, message_type, command_data.arg_string)
