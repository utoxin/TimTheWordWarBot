from irc.client import Event

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class LickCommand(BaseCommand):
    user_commands = {'lick'}

    def process(self, event: Event, command_data: CommandData) -> None:
        if self._execution_checks(event, command_data):
            if command_data.arg_count > 0:
                target = command_data.arg_string
            else:
                target = command_data.issuer

            self.lick_command(event, command_data, target)

    def lick_command(self, event: Event, command_data: CommandData, target: str) -> None:
        lick_message = text_generator.get_string(
                "licks [target]. Tastes like [flavor].", {
                    'source': command_data.issuer,
                    'target': target
                }
        )

        self.send_action(event, lick_message)
