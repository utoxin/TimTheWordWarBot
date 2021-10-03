import random
import re
from typing import Optional

from irc.client import Event

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class GetCommands(BaseCommand):
    user_commands = {'get', 'getfor', 'getfrom'}
    amusement_commands = {'getfor'}
    amusement_requires_target = True

    command_flag_map = {
        'get': 'get',
        'getfor': 'get',
        'getfrom': 'get'
    }

    def process(self, event: Event, command_data: CommandData) -> None:
        if self._execution_checks(event, command_data):
            if (command_data.command == 'getfor' or command_data.command == 'getfrom') and command_data.arg_count < 1:
                self.respond_to_user(event, f"Usage: !{command_data.command} <target> [<item>]")
                return

            if command_data.command == 'get':
                self.get_command(event, command_data)
            elif command_data.command == 'getfor':
                self.getfor_command(event, command_data)
            elif command_data.command == 'getfrom':
                self.getfrom_command(event, command_data)

    def get_command(self, event: Event, command_data: CommandData) -> None:
        self.get_item_message(event, command_data.issuer, command_data.arg_string)

    def getfor_command(self, event: Event, command_data: CommandData) -> None:
        target = command_data.args[0]

        if command_data.arg_count > 1:
            item = " ".join(command_data.args[1:])
        else:
            item = None

        self.get_item_message(event, target, item)

    def getfrom_command(self, event: Event, command_data: CommandData) -> None:
        target = command_data.args[0]
        recipient = command_data.issuer

        if command_data.arg_count > 1:
            item = " ".join(command_data.args[1:])
        else:
            item = None

        self.getfrom_item_message(event, target, recipient, item)

    def get_item_message(self, event, target: str, item: Optional[str]) -> None:
        if item is not None and item != "":
            if "spoon" in item.lower():
                self.send_action(event, "rummages around in the back room for a bit, then calls out, \"Sorry... there "
                                        "is no spoon. Maybe this will do...\"")
                item = text_generator.get_string("[item]", {})

            elif re.search(r"(\W|^)rum(\W|$)", item) and random.randint(0, 100) < 90:
                self.send_action(event, "rummages around in the back room for a bit, then calls out, \"All the rum is "
                                        "gone. I have this instead...\"")
                item = text_generator.get_string("[item]", {})

            elif random.randint(0, 100) < 50:
                self.send_action(event, "rummages around in the back room for a bit, then calls out. \"Sorry... I don't"
                                        " think I have that. Maybe this will do...\"")
                item = text_generator.get_string("[item]", {})

        else:
            item = text_generator.get_string("[item]", {})

        self.send_action(event, f"gets {target} {item}.")

    def getfrom_item_message(self, event, target: str, recipient: str, item: Optional[str]) -> None:
        if item is not None and item != "":
            if "spoon" in item.lower():
                self.send_action(event, f"rummages around in the {target}'s things for a bit, then calls out. "
                                        f"\"Sorry... they have no spoons. Maybe this will do...\"")
                item = text_generator.get_string("[item]", {})

            elif re.search(r"(\\W|^)rum(\\W|$)", item) and random.randint(0, 100) < 90:
                self.send_action(event, f"rummages around in {target}'s pantry for a bit, then calls out, \"All their "
                                        f"rum is gone. I found this instead...\"")
                item = text_generator.get_string("[item]", {})

            elif random.randint(0, 100) < 50:
                self.send_action(event, f"rummages around in {target}'s things for a bit, then calls out. \"Sorry... I "
                                        f"don't think I have that. Maybe this will do...\"")
                item = text_generator.get_string("[item]", {})

        else:
            item = text_generator.get_string("[item]", {})

        self.send_action(event, f"takes {item} from {target}, and gives it to {recipient}.")
