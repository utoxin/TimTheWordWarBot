import random
import threading

from irc.client import Event

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class FridgeCommand(BaseCommand):
    user_commands = {'fridge'}
    amusement_commands = {'fridge'}
    amusement_requires_target = True

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

    def process(self, event: Event, command_data: CommandData) -> None:
        if self._execution_checks(event, command_data):
            if command_data.arg_count > 0:
                target = command_data.arg_string
            else:
                target = command_data.issuer

            self.fridge_command(event, command_data, target)

    def fridge_command(
            self, event: Event, command_data: CommandData, target: str
    ) -> None:
        initial_delay = random.random() + 0.5
        initial_message = text_generator.get_string(
                "[start_message]", {
                    'start_message': self.start_messages
                }
        )

        x = threading.Timer(initial_delay, self._timer_thread, args = (event, initial_message))
        x.start()

        second_message = text_generator.get_string(
                "[throw_message]", {
                    'throw_message': self.throw_messages,
                    'thrower':       command_data.issuer,
                    'target':        target
                }
        )

        secondary_delay = initial_delay + 0.5 + random.random() * 2
        y = threading.Timer(secondary_delay, self._timer_thread, args = (event, second_message))
        y.start()

    def _timer_thread(self, event: Event, message: str) -> None:
        self.send_action(event, message)
