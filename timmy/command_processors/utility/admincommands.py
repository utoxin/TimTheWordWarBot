from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.db_access import channel_db


class AdminCommands(BaseCommand):
    admin_commands = ['ignore', 'unignore', 'listignores', 'shout']

    help_topics = [('admin', 'core admin commands', '$ignore <username>', 'Places user on the admin-ignore list. They '
                                                                          'can not remove themselves from that list.'),
                   ('admin', 'core admin commands', '$unignore <username>', 'Remove user from all ignore lists.'),
                   ('admin', 'core admin commands', '$listignores', 'Lists all admin ignores.')]

    def process(self, command_data: CommandData):
        from timmy.core import user_perms, bot_instance

        if command_data.command == 'shout':
            channel_groups = channel_db.get_channel_groups()

            if command_data.arg_count == 0:
                self.respond_to_user(command_data, "Usage: $shout <message>")
                self.respond_to_user(command_data, "USage: $shout @<channelgroup> <message>")
                return
            else:
                destination = 'all'
                message = command_data.arg_string

                if command_data.args[0].startswith("@"):
                    if command_data.arg_count == 1:
                        self.respond_to_user(command_data, "USage: $shout @<channelgroup> <message>")
                        return
                    else:
                        destination = command_data.args[0][1:]
                        message = " ".join(command_data.args[1:])

                if destination == 'all':
                    from timmy.core import bot_instance
                    target_channels = bot_instance.channels
                else:
                    if destination not in channel_groups:
                        self.respond_to_user(command_data, "Channel group not found.")
                        return
                    else:
                        target_channels = channel_groups[destination]

                for channel in target_channels:
                    bot_instance.connection.privmsg(channel, f"{command_data.issuer} shouts @{destination}: {message}")

        elif command_data.command == 'ignore':
            if command_data.arg_count != 1:
                self.respond_to_user(command_data, "Usage: $ignore <user>")
            else:
                user_perms.add_ignore(command_data.args[0], 'admin')
                self.respond_to_user(command_data, "User added to admin ignore list.")

        elif command_data.command == 'unignore':
            if command_data.arg_count != 1:
                self.respond_to_user(command_data, "Usage: $unignore <user>")
            else:
                user_perms.remove_ignore(command_data.args[0], 'admin')
                self.respond_to_user(command_data, "User removed from admin ignore list.")

        elif command_data.command == 'listignores':
            self.respond_to_user(command_data, f"There are {len(user_perms.admin_ignores)} users on the admin ignore "
                                               f"list. Sending the list in private.")

            self.pm_user(command_data, "Ignored users: " + ", ".join(user_perms.admin_ignores.keys()))
        return
