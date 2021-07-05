from timmy import core, db_access
from timmy.command_processors import interaction_controls
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData


class AttackCommand(BaseCommand):
    user_commands = {'attack'}

    def process(self, connection, event, command_data: CommandData):
        user_data = db_access.user_directory.find_user_data(command_data.issuer)

        if command_data.in_pm:
            self.respond_to_user(connection, event, "You can't do that in a private message.")
            return

        channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

        if not channel_data.command_settings['attack']:
            self.respond_to_user(connection, event, "I'm sorry, I don't do that here.")
            return

        if command_data.arg_count > 0:
            target = command_data.arg_string

            if interaction_controls.interact_with_user(target, command_data.command):
                self.attack_command(connection, event, command_data, target)
            else:
                self.respond_to_user(connection, event, "I'm sorry, it's been requested that I not do that.")
        else:
            if interaction_controls.interact_with_user(command_data.issuer, command_data.command):
                self.attack_command(connection, event, command_data, command_data.issuer)
            else:
                self.respond_to_user(connection, event, "I'm sorry, it's been requested that I not do that.")

        return

    def attack_command(self, connection, event, command_data: CommandData, target):
        item =
