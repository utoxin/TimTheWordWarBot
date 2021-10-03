from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.db_access import channel_db


class ChannelGroupCommands(BaseCommand):
    admin_commands = {'channelgroup'}
    sub_commands = {'list', 'add', 'remove', 'destroy'}

    def process(self, command_data: CommandData) -> None:
        self.handle_subcommand(command_data)

    def _list_handler(self, command_data: CommandData) -> None:
        channel_groups = channel_db.get_channel_groups()

        if command_data.arg_count == 1:
            if len(channel_groups) > 0:
                channel_group_str = ", ".join(list(channel_groups.keys()))
                self.respond_to_user(command_data, f"Channel Groups: {channel_group_str}")
            else:
                self.respond_to_user(command_data, f"No known channel groups.")
        elif command_data.arg_count == 2:
            if command_data.args[1] in channel_groups:
                channel_str = ", ".join(channel_groups[command_data.args[1]])
                self.respond_to_user(command_data, f"Channels in group: {channel_str}")
            else:
                self.respond_to_user(command_data, "Unknown channel group.")
        else:
            self.respond_to_user(command_data, "Usage: $channelgroup list [<list name>]")

    def _add_handler(self, command_data: CommandData) -> None:
        if command_data.arg_count == 3:
            channel_groups = channel_db.get_channel_groups()

            group = command_data.args[1]
            channel = command_data.args[2]

            from timmy.core import bot_instance
            if group in channel_groups and channel in channel_groups[group]:
                self.respond_to_user(command_data, "Channel is already in that group.")
            elif channel not in bot_instance.channels:
                self.respond_to_user(command_data, "Channel not found.")
            else:
                channel_db.add_to_channel_group(group, channel)
                self.respond_to_user(command_data, f"Channel added to group: {group}")
        else:
            self.respond_to_user(command_data, "Usage: $channelgroup add <group> <channel>")

    def _remove_handler(self, command_data: CommandData) -> None:
        if command_data.arg_count == 3:
            channel_groups = channel_db.get_channel_groups()

            group = command_data.args[1]
            channel = command_data.args[2]

            from timmy.core import bot_instance
            if group not in channel_groups:
                self.respond_to_user(command_data, "Channel group not found.")
            elif channel not in bot_instance.channels:
                self.respond_to_user(command_data, "Channel not found.")
            elif channel not in channel_groups[group]:
                self.respond_to_user(command_data, "Channel isn't in that group.")
            else:
                channel_db.remove_from_channel_group(group, channel)
                self.respond_to_user(command_data, f"Channel removed from group: {group}")
        else:
            self.respond_to_user(command_data, "Usage: $channelgroup remove <group> <channel>")

    def _destroy_handler(self, command_data: CommandData) -> None:
        if command_data.arg_count == 2:
            channel_groups = channel_db.get_channel_groups()

            group = command_data.args[1]

            if group not in channel_groups:
                self.respond_to_user(command_data, "Channel group not found.")
            else:
                channel_db.destroy_channel_group(group)
                self.respond_to_user(command_data, f"Channel group destroyed: {group}")
        else:
            self.respond_to_user(command_data, "Usage: $channelgroup destroy <group>")
