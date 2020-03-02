from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class RaptorCommands(BaseCommand):
    user_commands = {"raptor", "testwhois"}
    sub_commands = {"adopt", "release", "details", "rename"}

    def process(self, connection, event, command_data: CommandData):
        if command_data.command == "testwhois":
            connection.whois(command_data.issuer)
            return

        self.handle_subcommand(connection, event, command_data)