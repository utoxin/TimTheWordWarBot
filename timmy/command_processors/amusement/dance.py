import random

from irc.client import ServerConnection, Event

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class DanceCommand(BaseCommand):
    user_commands = {'dance'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if not command_data.in_pm:
            channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

            if not channel_data.command_settings[command_data.command]:
                self.respond_to_user(connection, event, "I'm sorry, I don't do that here.")
                return

        self.dance_command(connection, event)

    def dance_command(self, connection: ServerConnection, event: Event) -> None:
        dance = text_generator.get_string('[dance]')

        random_check = random.randint(0, 100)

        if random_check > 90:
            message = f"dances the {dance} so well he should be on Dancing with the Stars!"
        elif random_check > 60:
            message = f"does the {dance}, and tears up the dance floor."
        elif random_check > 30:
            message = f"attempts to do the {dance}, but obviously needs more practice."
        else:
            message = f"flails about in a fashion that vaguely resembles the {dance}. Sort of."

        self.send_action(connection, event, message)

