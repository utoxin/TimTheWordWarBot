import random
import threading

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class DefenestrateCommand(BaseCommand):
    user_commands = {'defenestrate'}
    amusement_commands = {'defenestrate'}
    amusement_requires_target = True

    def process(self, command_data: CommandData) -> None:
        if self._execution_checks(command_data):
            if command_data.arg_count > 0:
                target = command_data.arg_string
            else:
                target = command_data.issuer

            self.defenestrate_command(command_data, target)

    def defenestrate_command(self, command_data: CommandData, target: str) -> None:
        initial_delay = random.random() + 0.5
        initial_message = text_generator.get_string("[defenestration_starter]")

        x = threading.Timer(initial_delay, self._timer_thread, args = [command_data, initial_message])
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
        y = threading.Timer(secondary_delay, self._timer_thread, args = [command_data, second_message])
        y.start()

    def _timer_thread(self, command_data: CommandData, message: str) -> None:
        self.send_action(command_data, message)
