import random
import threading

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class HerdCommand(BaseCommand):
    user_commands = ['herd']
    amusement_commands = ['herd']
    amusement_requires_target = True

    def process(self, command_data: CommandData) -> None:
        if self._execution_checks(command_data):
            if command_data.arg_count > 0:
                target = command_data.arg_string
            else:
                target = command_data.issuer

            self.herd_command(command_data, target)

    def herd_command(self, command_data: CommandData, target: str) -> None:
        box_message = text_generator.get_string(
                "collects several [color] boxes, and lays them around to attract "
                "cats..."
        )

        self.send_action(command_data, box_message)

        option = random.randrange(100)

        if option > 33:
            herd_message = text_generator.get_string(
                    "[cat_herd]", {
                        'source': command_data.issuer,
                        'target': target
                    }
            )
        elif option > 11 and target != command_data.issuer:
            herd_message = text_generator.get_string(
                    "gets confused and, [cat_herd]", {
                        'source': command_data.issuer,
                        'target': command_data.issuer
                    }
            )
        else:
            herd_message = "can't seem to find any cats. Maybe he used the wrong color of box?"

        x = threading.Timer(2, self._timer_thread, args = [command_data, herd_message])
        x.start()

    def _timer_thread(self, command_data: CommandData, message: str) -> None:
        self.send_action(command_data, message)
