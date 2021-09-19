import sys
import time

from irc.client import Event, ServerConnection

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.db_access import channel_db


class AdminCommands(BaseCommand):
    admin_commands = {'shutdown', 'ignore', 'unignore', 'listignores', 'shout'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData):
        from timmy.core import user_perms

        if command_data.command == 'shutdown':
            if command_data.issuer_data.global_admin:
                self.respond_to_user(connection, event, "Shutting down.........")
                connection.quit("Help, help! I'm being repressed!")
                time.sleep(1)
                sys.exit(0)
            else:
                self.respond_to_user(connection, event, "Only a global admin can do that!")

        elif command_data.command == 'shout':
            channel_groups = channel_db.get_channel_groups()

            if command_data.arg_count == 0:
                self.respond_to_user(connection, event, "Usage: $shout <message>")
                self.respond_to_user(connection, event, "USage: $shout @<channelgroup> <message>")
                return
            else:
                destination = 'all'
                message = command_data.arg_string

                if command_data.args[0].startswith("@"):
                    if command_data.arg_count == 1:
                        self.respond_to_user(connection, event, "USage: $shout @<channelgroup> <message>")
                        return
                    else:
                        destination = command_data.args[0][1:]
                        message = " ".join(command_data.args[1:])

                if destination == 'all':
                    from timmy.core import bot_instance
                    target_channels = bot_instance.channels
                else:
                    if destination not in channel_groups:
                        self.respond_to_user(connection, event, "Channel group not found.")
                        return
                    else:
                        target_channels = channel_groups[destination]

                for channel in target_channels:
                    connection.privmsg(channel, f"{command_data.issuer} shouts @{destination}: {message}")

        elif command_data.command == 'ignore':
            if command_data.arg_count != 1:
                self.respond_to_user(connection, event, "Usage: $ignore <user>")
            else:
                user_perms.add_ignore(command_data.args[0], 'admin')
                self.respond_to_user(connection, event, "User added to admin ignore list.")

        elif command_data.command == 'unignore':
            if command_data.arg_count != 1:
                self.respond_to_user(connection, event, "Usage: $unignore <user>")
            else:
                user_perms.remove_ignore(command_data.args[0], 'admin')
                self.respond_to_user(connection, event, "User removed from admin ignore list.")

        elif command_data.command == 'listignores':
            self.respond_to_user(connection, event, f"There are {len(user_perms.admin_ignores)} users on the admin "
                                                    f"ignore list. Sending the list in private.")

            connection.privmsg(command_data.issuer, "Ignored users: " + ", ".join(user_perms.admin_ignores.keys()))

        return
