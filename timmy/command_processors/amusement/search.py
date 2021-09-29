import random

from irc.client import Event, ServerConnection

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class SearchCommand(BaseCommand):
    user_commands = {'search'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if self._execution_checks(connection, event, command_data):
            target = command_data.issuer

            if command_data.arg_count > 0:
                target = command_data.args[0]

            self.search_command(connection, event, command_data, target)

    def search_command(self, connection: ServerConnection, event: Event, command_data: CommandData, target: str
                       ) -> None:
        if target != command_data.issuer and random.randint(0, 100) < 25:
            self.send_action(connection, event, f"decides at the last second to search {command_data.issuer}'s things "
                                                f"instead ...")
            target = command_data.issuer
        else:
            self.send_action(connection, event, f"searches through {target}'s things, looking for contraband...")

        found_items = random.randint(0, 4)

        if found_items == 0:
            self.send_action(connection, event, f"can't find anything, and grudgingly clears {target}.")
            return
        elif found_items == 1:
            item_string = text_generator.get_string("[item]")
        else:
            items = [text_generator.get_string("[item]") for x in range(found_items)]

            item_string = ""

            for x in range(found_items):
                if x > 0 and found_items > 2:
                    item_string += ","

                if x == found_items - 1:
                    item_string += " and "
                elif x > 0:
                    item_string += " "

                item_string += items[x]

        self.send_action(connection, event, f"reports {target} to Skynet for possession of {item_string}.")
