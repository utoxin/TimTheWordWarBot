from irc.client import Event, ServerConnection

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class IgnoreCommands(BaseCommand):
    user_commands = {'ignore', 'unignore'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData):
        from timmy.core import user_perms

        if command_data.command == 'ignore':
            if command_data.arg_count != 1 or command_data.args[0] not in ['soft', 'hard']:
                self.respond_to_user(connection, event, "Usage: !ignore <soft|hard>")
            else:
                user_perms.add_ignore(command_data.issuer, command_data.args[0])
                self.respond_to_user(connection, event, f"User added to {command_data.args[0]} ignore list.")

        elif command_data.command == 'unignore':
            if command_data.arg_count != 0:
                self.respond_to_user(connection, event, "Usage: !unignore")
            else:
                user_perms.remove_ignore(command_data.issuer, 'soft')
                user_perms.remove_ignore(command_data.issuer, 'hard')
                self.respond_to_user(connection, event, "User removed from ignore lists.")

        return
