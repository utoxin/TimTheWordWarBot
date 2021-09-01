import random
import threading

from irc.client import ServerConnection, Event

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class FridgeCommand(BaseCommand):
    user_commands = {'fridge'}

    start_messages = {
        "looks back and forth, then slinks off...",
        "slips into the kitchen to grab something...",
        "places an order with the local appliance store..."
    }

    throw_messages = {
        "hurls {a} [color] fridge at [target].",
        "hurls {a} [color] fridge at [thrower] and runs away giggling.",
        "trips and drops {a} [color] fridge on himself.",
        "rigs a complicated mechanism, and drops {a} [color] fridge onto [target]",
        "tries to build a complicated mechanism, but it breaks, and {a} [color] fridge squishes him.",
        "picks the wrong target, and launches {a} [color] fridge at [thrower] with a trebuchet.",
        "grabs {a} [color] fridge, but forgets to empty it first. What a mess!"
    }

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
                self.fridge_command(connection, event, command_data, target)
            else:
                self.respond_to_user(connection, event, "I'm sorry, it's been requested that I not do that.")
        else:
            if interaction_controls.interact_with_user(command_data.issuer, command_data.command):
                self.fridge_command(connection, event, command_data, command_data.issuer)
            else:
                self.respond_to_user(connection, event, "I'm sorry, it's been requested that I not do that.")

    def fridge_command(self, connection: ServerConnection, event: Event, command_data: CommandData,
                       target: str) -> None:
        initial_delay = random.random() + 0.5
        initial_message = text_generator.get_string("[start_message]", {
            'start_message': self.start_messages
        })

        x = threading.Timer(initial_delay, self._timer_thread, args=(connection, event, initial_message))
        x.start()

        second_message = text_generator.get_string("[throw_message]", {
            'throw_message': self.throw_messages,
            'thrower': command_data.issuer,
            'target': target
        })

        secondary_delay = initial_delay + 0.5 + random.random() * 2
        y = threading.Timer(secondary_delay, self._timer_thread, args=(connection, event, second_message))
        y.start()

    def _timer_thread(self, connection: ServerConnection, event: Event, message: str) -> None:
        self.send_action(connection, event, message)
