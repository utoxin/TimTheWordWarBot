import random
import re
from typing import Optional

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class GetCommands(BaseCommand):
    user_commands = ['get', 'getfor', 'getfrom']
    amusement_commands = ['getfor']
    amusement_requires_target = True

    command_flag_map = {
        'get': 'get',
        'getfor': 'get',
        'getfrom': 'get'
    }

    help_topics = [('user', 'amusement commands', '!get [<anything>]', 'I will fetch you what you ask for. Maybe.'),
                   ('user', 'amusement commands', '!getfor <someone> [<anything>]', 'I will get something for someone '
                                                                                    'else.')]

    def process(self, command_data: CommandData) -> None:
        if self._execution_checks(command_data):
            if (command_data.command == 'getfor' or command_data.command == 'getfrom') and command_data.arg_count < 1:
                self.respond_to_user(command_data, f"Usage: !{command_data.command} <target> [<item>]")
                return

            if command_data.command == 'get':
                self.get_command(command_data)
            elif command_data.command == 'getfor':
                self.getfor_command(command_data)
            elif command_data.command == 'getfrom':
                self.getfrom_command(command_data)

    def get_command(self, command_data: CommandData) -> None:
        self.get_item_message(command_data, command_data.issuer, command_data.arg_string)

    def getfor_command(self, command_data: CommandData) -> None:
        target = command_data.args[0]

        if command_data.arg_count > 1:
            item = " ".join(command_data.args[1:])
        else:
            item = None

        self.get_item_message(command_data, target, item)

    def getfrom_command(self, command_data: CommandData) -> None:
        target = command_data.args[0]
        recipient = command_data.issuer

        if command_data.arg_count > 1:
            item = " ".join(command_data.args[1:])
        else:
            item = None

        self.getfrom_item_message(command_data, target, recipient, item)

    def get_item_message(self, command_data: CommandData, target: str, item: Optional[str]) -> None:
        if item is not None and item != "":
            if "spoon" in item.lower():
                self.send_action(command_data, "rummages around in the back room for a bit, then calls out, \"Sorry... "
                                               "there is no spoon. Maybe this will do...\"")
                item = text_generator.get_string("[item]", {})

            elif re.search(r"(\W|^)rum(\W|$)", item) and random.randint(0, 100) < 90:
                self.send_action(command_data, "rummages around in the back room for a bit, then calls out, \"All the "
                                               "rum is gone. I have this instead...\"")
                item = text_generator.get_string("[item]", {})

            elif random.randint(0, 100) < 50:
                self.send_action(command_data, "rummages around in the back room for a bit, then calls out. \"Sorry... "
                                               "I don't think I have that. Maybe this will do...\"")
                item = text_generator.get_string("[item]", {})

        else:
            item = text_generator.get_string("[item]", {})

        self.send_action(command_data, f"gets {target} {item}.")

    def getfrom_item_message(self, command_data: CommandData, target: str, recipient: str, item: Optional[str]) -> None:
        if item is not None and item != "":
            if "spoon" in item.lower():
                self.send_action(command_data, f"rummages around in the {target}'s things for a bit, then calls out. "
                                               f"\"Sorry... they have no spoons. Maybe this will do...\"")
                item = text_generator.get_string("[item]", {})

            elif re.search(r"(\\W|^)rum(\\W|$)", item) and random.randint(0, 100) < 90:
                self.send_action(command_data, f"rummages around in {target}'s pantry for a bit, then calls out, \"All "
                                               f"their rum is gone. I found this instead...\"")
                item = text_generator.get_string("[item]", {})

            elif random.randint(0, 100) < 50:
                self.send_action(command_data, f"rummages around in {target}'s things for a bit, then calls out. "
                                               f"\"Sorry... I don't think I have that. Maybe this will do...\"")
                item = text_generator.get_string("[item]", {})

        else:
            item = text_generator.get_string("[item]", {})

        self.send_action(command_data, f"takes {item} from {target}, and gives it to {recipient}.")
