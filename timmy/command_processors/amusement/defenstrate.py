import random
import threading

from irc.client import Event, ServerConnection

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class DefenestrateCommand(BaseCommand):
    user_commands = {'defenestrate'}

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
                self.defenestrate_command(connection, event, command_data, target)
            else:
                self.respond_to_user(connection, event, "I'm sorry, it's been requested that I not do that.")
        else:
            if interaction_controls.interact_with_user(command_data.issuer, command_data.command):
                self.defenestrate_command(connection, event, command_data, command_data.issuer)
            else:
                self.respond_to_user(connection, event, "I'm sorry, it's been requested that I not do that.")

    def defenestrate_command(
            self, connection: ServerConnection, event: Event, command_data: CommandData, target: str
    ) -> None:
        initial_delay = random.random() + 0.5
        initial_message = text_generator.get_string("[defenestration_starter]")

        x = threading.Timer(initial_delay, self._timer_thread, args = (connection, event, initial_message))
        x.start()

        random_choice = random.randint(0, 100)

        if random_choice > 33:
            second_message = text_generator.get_string(
                    "[defenestration_success]", {
                        'target': target
                    }
            )
        else:
            second_message = text_generator.get_string(
                    "[defenestration_failure]", {
                        'target': command_data.issuer
                    }
            )

        secondary_delay = initial_delay + 0.5 + random.random() * 2
        y = threading.Timer(secondary_delay, self._timer_thread, args = (connection, event, second_message))
        y.start()

    def _timer_thread(self, connection: ServerConnection, event: Event, message: str) -> None:
        self.send_action(connection, event, message)
