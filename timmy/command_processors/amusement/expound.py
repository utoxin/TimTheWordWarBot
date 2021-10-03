import random

from irc.client import Event

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData


class ExpoundCommand(BaseCommand):
    user_commands = {'expound'}
    interaction_checks = False
    allowed_in_pm = False

    def process(self, event: Event, command_data: CommandData) -> None:
        if self._execution_checks(event, command_data):
            from timmy.utilities import markov_generator

            channel_data: ChannelData = core.bot_instance.channels[command_data.channel]
            choice = random.randrange(0, 100)
            message_type = 'say'

            if choice < 5 or (choice < 10 and command_data.arg_count > 0):
                message_type = 'mutter'
            elif choice < 20:
                message_type = 'emote'
            elif choice < 40:
                message_type = 'novel'

            markov_generator.random_action(channel_data, message_type, command_data.arg_string)
