from irc.client import ServerConnection, Event

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData


class PingCommand(BaseCommand):
    user_commands = {'ping'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if not command_data.in_pm:
            channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

            if not channel_data.command_settings['ping']:
                self.respond_to_user(connection, event, "I'm sorry, I don't do that here.")
                return

        self.respond_to_user(connection, event, "Pong!")
