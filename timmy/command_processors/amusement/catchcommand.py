from irc.client import ServerConnection, Event

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class CatchCommand(BaseCommand):
    user_commands = {'catch'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if not command_data.in_pm:
            channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

            if not channel_data.command_settings['dice']:
                self.respond_to_user(connection, event, "I'm sorry, I don't do that here.")
                return

        target = '[pokemon]'

        if command_data.arg_count > 0:
            target = command_data.arg_string

        target = text_generator.get_string(target, {
            'source': command_data.issuer
        })

        initial_message = text_generator.get_string("[catch_initial]", {
            'source': command_data.issuer,
            'target': target
        })

        catch_message = text_generator.get_string("[catch_result]", {
            'source': command_data.issuer,
            'target': target
        })

        self.send_action(connection, event, initial_message)
        self.send_action(connection, event, catch_message)

        return
