import random

from irc.client import ServerConnection, Event

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData


class PickOneCommand(BaseCommand):
    user_commands = {'pickone'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if not command_data.in_pm:
            channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

            if not channel_data.command_settings['pickone']:
                self.respond_to_user(connection, event, "I'm sorry, I don't do that here.")
                return

        if command_data.arg_count == 0:
            self.respond_to_user(connection, event, "Usage: !pickone comma, separated, choices")
            return

        choices = command_data.arg_string.split(',')

        if len(choices) == 1:
            self.respond_to_user(connection, event, "Well, there's not much choice there...")
        else:
            choice = choices[random.randrange(len(choices))]
            self.respond_to_user(connection, event, f"I choose... {choice}")