import random
import threading

from irc.client import Event

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class DefenestrateCommand(BaseCommand):
    user_commands = {'defenestrate'}
    amusement_commands = {'defenestrate'}
    amusement_requires_target = True

    def process(self, event: Event, command_data: CommandData) -> None:
        if self._execution_checks(event, command_data):
            if command_data.arg_count > 0:
                target = command_data.arg_string
            else:
                target = command_data.issuer

            self.defenestrate_command(event, command_data, target)

    def defenestrate_command(self, event: Event, command_data: CommandData, target: str) -> None:
        initial_delay = random.random() + 0.5
        initial_message = text_generator.get_string("[defenestration_starter]")

        x = threading.Timer(initial_delay, self._timer_thread, args = (event, initial_message))
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
        y = threading.Timer(secondary_delay, self._timer_thread, args = (event, second_message))
        y.start()

    def _timer_thread(self, event: Event, message: str) -> None:
        self.send_action(event, message)
