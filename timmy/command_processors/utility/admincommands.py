import sys
import time

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy import core


class AdminCommands(BaseCommand):
    admin_commands = {'test', 'shutdown'}

    def process(self, connection, event, command_data: CommandData):
        if command_data.command == 'test':
            if core.user_perms.is_admin(command_data.issuer, command_data.channel):
                self.respond_to_user(connection, event, "You're an admin!")
            else:
                self.respond_to_user(connection, event, "Don't try and trick me!")

        elif command_data.command == 'shutdown':
            if command_data.issuer_data.global_admin:
                self.respond_to_user(connection, event, "Shutting down.........")
                connection.quit("Help, help! I'm being repressed!")
                time.sleep(1)
                sys.exit(0)
            else:
                self.respond_to_user(connection, event, "Only a global admin can do that!")

        return
