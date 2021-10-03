from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class PingCommand(BaseCommand):
    user_commands = {'ping'}
    interaction_checks = False

    def process(self, command_data: CommandData) -> None:
        if self._execution_checks(command_data):
            self.respond_to_user(command_data, "Pong!")
