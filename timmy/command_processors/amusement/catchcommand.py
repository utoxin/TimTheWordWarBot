from irc.client import Event, ServerConnection

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class CatchCommand(BaseCommand):
    user_commands = {'catch'}
    amusement_commands = {'catch'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if self._execution_checks(connection, event, command_data):
            target = '[pokemon]'

            if command_data.arg_count > 0:
                target = command_data.arg_string

            target = text_generator.get_string(
                    target, {
                        'source': command_data.issuer
                    }
            )

            initial_message = text_generator.get_string(
                    "[catch_initial]", {
                        'source': command_data.issuer,
                        'target': target
                    }
            )

            catch_message = text_generator.get_string(
                    "[catch_result]", {
                        'source': command_data.issuer,
                        'target': target
                    }
            )

            self.send_action(connection, event, initial_message)
            self.send_action(connection, event, catch_message)
