from irc.client import Event, ServerConnection

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class CommandmentCommand(BaseCommand):
    user_commands = {'commandment'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if not command_data.in_pm:
            channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

            if not channel_data.command_settings['commandment']:
                self.respond_to_user(connection, event, "I'm sorry, I don't do that here.")
                return

        commandment = ""

        if command_data.arg_count > 0:
            try:
                commandment = int(command_data.args[0])
            except TypeError:
                self.respond_to_user(connection, event, "Could not understand first parameter. Was it numeric?")
                return

        commandment_message = text_generator.get_string(f"[commandment{commandment}]", {})

        self.respond_to_user(connection, event, commandment_message)
