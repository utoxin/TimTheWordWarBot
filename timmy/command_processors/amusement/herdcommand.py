import random
import threading

from irc.client import ServerConnection, Event

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class HerdCommand(BaseCommand):
    user_commands = {'herd'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        from timmy.command_processors import interaction_controls

        if not command_data.in_pm:
            channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

            if not channel_data.command_settings[command_data.command]:
                self.respond_to_user(connection, event, "I'm sorry, I don't do that here.")
                return

        if command_data.arg_count > 0:
            target = command_data.arg_string

            if interaction_controls.interact_with_user(target, command_data.command):
                self.herd_command(connection, event, command_data, target)
            else:
                self.respond_to_user(connection, event, "I'm sorry, it's been requested that I not do that.")
        else:
            if interaction_controls.interact_with_user(command_data.issuer, command_data.command):
                self.herd_command(connection, event, command_data, command_data.issuer)
            else:
                self.respond_to_user(connection, event, "I'm sorry, it's been requested that I not do that.")

    def herd_command(self, connection: ServerConnection, event: Event, command_data: CommandData, target: str) -> None:
        box_message = text_generator.get_string("collects several [color] boxes, and lays them around to attract "
                                                "cats...")

        self.send_action(connection, event, box_message)

        option = random.randrange(100)

        if option > 33:
            herd_message = text_generator.get_string("[cat_herd]", {
                'source': command_data.issuer,
                'target': target
            })
        elif option > 11 and target != command_data.issuer:
            herd_message = text_generator.get_string("gets confused and, [cat_herd]", {
                'source': command_data.issuer,
                'target': command_data.issuer
            })
        else:
            herd_message = "can't seem to find any cats. Maybe he used the wrong color of box?"

        x = threading.Timer(2, self._timer_thread, args=(connection, event, herd_message))
        x.start()

    def _timer_thread(self, connection: ServerConnection, event: Event, message: str) -> None:
        self.send_action(connection, event, message)
