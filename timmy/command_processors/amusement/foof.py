import random
import threading

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class FoofCommand(BaseCommand):
    user_commands = ['foof']
    amusement_commands = ['foof']
    amusement_requires_target = True

    def process(self, command_data: CommandData) -> None:
        if self._execution_checks(command_data):
            if command_data.arg_count > 0:
                target = command_data.arg_string
            else:
                target = command_data.issuer

            self.foof_command(command_data, target)

    def foof_command(self, command_data: CommandData, target: str) -> None:
        initial_delay = random.random() + 0.5
        initial_message = "surreptitiously works his way over to the couch, looking ever so casual..."

        x = threading.Timer(initial_delay, self._timer_thread, args = [command_data, initial_message])
        x.start()

        option = random.randrange(100)

        if option > 33:
            second_message = text_generator.get_string(
                    "grabs a [color] pillow, and throws it at [target], hitting "
                    "them squarely in the back of the head.", {
                        'source': command_data.issuer,
                        'target': target
                    }
            )

        elif option > 11 and target != command_data.issuer:
            second_message = text_generator.get_string(
                    "laughs maniacally then throws a [color] pillow at [target], "
                    "then runs off and hides behind the nearest couch.", {
                        'source': command_data.issuer,
                        'target': command_data.issuer
                    }
            )
        else:
            second_message = text_generator.get_string("trips and lands on a [color] pillow. Oof!")

        secondary_delay = initial_delay + 0.5 + random.random() * 2
        y = threading.Timer(secondary_delay, self._timer_thread, args = [command_data, second_message])
        y.start()

    def _timer_thread(self, command_data: CommandData, message: str) -> None:
        self.send_action(command_data, message)
