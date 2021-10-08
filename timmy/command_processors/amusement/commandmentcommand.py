from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class CommandmentCommand(BaseCommand):
    user_commands = ['commandment']
    interaction_checks = False

    def process(self, command_data: CommandData) -> None:
        if self._execution_checks(command_data):
            commandment = ""

            if command_data.arg_count > 0:
                try:
                    commandment = int(command_data.args[0])
                except TypeError:
                    self.respond_to_user(command_data, "Could not understand first parameter. Was it numeric?")
                    return

            commandment_message = text_generator.get_string(f"[commandment{commandment}]", {})

            self.respond_to_user(command_data, commandment_message)
