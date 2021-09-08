import random
import re
from typing import Optional

from irc.client import Event, ServerConnection

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class GetCommands(BaseCommand):
    user_commands = {'get', 'getfor', 'getfrom'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        from timmy.command_processors import interaction_controls

        if not command_data.in_pm:
            channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

            if not channel_data.command_settings['get']:
                self.respond_to_user(connection, event, "I'm sorry, I don't do that here.")
                return

        if (command_data.command == 'getfor' or command_data.command == 'getfrom') and command_data.arg_count < 1:
            self.respond_to_user(connection, event, f"Usage: !{command_data.command} <target> [<item>]")
            return

        if command_data.arg_count > 0:
            target = command_data.args[0]

            if not interaction_controls.interact_with_user(target, 'get'):
                self.respond_to_user(connection, event, "I'm sorry, it's been requested that I not do that.")
                return
        elif not interaction_controls.interact_with_user(command_data.issuer, 'get'):
            self.respond_to_user(connection, event, "I'm sorry, it's been requested that I not do that.")
            return

        if command_data.command == 'get':
            self.get_command(connection, event, command_data)
        elif command_data.command == 'getfor':
            self.getfor_command(connection, event, command_data)
        elif command_data.command == 'getfrom':
            self.getfrom_command(connection, event, command_data)

    def get_command(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        self.get_item_message(connection, event, command_data.issuer, command_data.arg_string)

    def getfor_command(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        target = command_data.args[0]

        if command_data.arg_count > 1:
            item = " ".join(command_data.args[1:])
        else:
            item = None

        self.get_item_message(connection, event, target, item)

    def getfrom_command(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        target = command_data.args[0]
        recipient = command_data.issuer

        if command_data.arg_count > 1:
            item = " ".join(command_data.args[1:])
        else:
            item = None

        self.getfrom_item_message(connection, event, target, recipient, item)

    def get_item_message(self, connection, event, target: str, item: Optional[str]) -> None:
        if item is not None and item != "":
            if "spoon" in item.lower():
                self.send_action(connection, event, "rummages around in the back room for a bit, then calls out, "
                                                    "\"Sorry... there is no spoon. Maybe this will do...\"")
                item = text_generator.get_string("[item]", {})

            elif re.search(r"(\W|^)rum(\W|$)", item) and random.randint(0, 100) < 90:
                self.send_action(connection, event, "rummages around in the back room for a bit, then calls out, \"All "
                                                    "the rum is gone. I have this instead...\"")
                item = text_generator.get_string("[item]", {})

            elif random.randint(0, 100) < 50:
                self.send_action(connection, event, "rummages around in the back room for a bit, then calls out. "
                                                    "\"Sorry... I don't think I have that. Maybe this will do...\"")
                item = text_generator.get_string("[item]", {})

        else:
            item = text_generator.get_string("[item]", {})

        self.send_action(connection, event, f"gets {target} {item}.")

    def getfrom_item_message(self, connection, event, target: str, recipient: str, item: Optional[str]) -> None:
        if item is not None and item != "":
            if "spoon" in item.lower():
                self.send_action(connection, event, f"rummages around in the {target}'s things for a bit, then calls "
                                                    f"out. \"Sorry... they have no spoons. Maybe this will do...\"")
                item = text_generator.get_string("[item]", {})

            elif re.search(r"(\\W|^)rum(\\W|$)", item) and random.randint(0, 100) < 90:
                self.send_action(connection, event, "rummages around in {target}'s pantry for a bit, then calls out, "
                                                    "\"All their rum is gone. I found this instead...\"")
                item = text_generator.get_string("[item]", {})

            elif random.randint(0, 100) < 50:
                self.send_action(connection, event, f"rummages around in {target}'s things for a bit, then calls out. "
                                                    "\"Sorry... I don't think I have that. Maybe this will do...\"")
                item = text_generator.get_string("[item]", {})

        else:
            item = text_generator.get_string("[item]", {})

        self.send_action(connection, event, f"takes {item} from {target}, and gives it to {recipient}.")
