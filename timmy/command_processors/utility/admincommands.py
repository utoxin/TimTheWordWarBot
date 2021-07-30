import sys
import time

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class AdminCommands(BaseCommand):
    admin_commands = {'shutdown', 'ignore', 'unignore', 'listignores', 'channelgroup'}

    def process(self, connection, event, command_data: CommandData):
        if command_data.command == 'shutdown':
            if command_data.issuer_data.global_admin:
                self.respond_to_user(connection, event, "Shutting down.........")
                connection.quit("Help, help! I'm being repressed!")
                time.sleep(1)
                sys.exit(0)
            else:
                self.respond_to_user(connection, event, "Only a global admin can do that!")

        return
