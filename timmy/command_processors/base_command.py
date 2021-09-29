from irc.client import Event, ServerConnection

from timmy import core
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData
from timmy.event_handlers import CommandHandler


class BaseCommand:
    user_commands = {}
    admin_commands = {}
    sub_commands = {}

    allowed_in_pm = True
    interaction_checks = True
    permitted_checks = True

    command_flag_map = {}

    @staticmethod
    def respond_to_user(connection: ServerConnection, event: Event, message: str) -> None:
        if event.type == "privmsg":
            connection.privmsg(event.source.nick, message)
        else:
            connection.privmsg(event.target, event.source.nick + ": " + message)

    @staticmethod
    def send_message(connection: ServerConnection, event: Event, message: str) -> None:
        if event.type == "privmsg":
            connection.privmsg(event.source.nick, message)
        else:
            connection.privmsg(event.target, message)

    @staticmethod
    def send_action(connection: ServerConnection, event: Event, message: str) -> None:
        if event.type == "privmsg":
            connection.action(event.source.nick, message)
        else:
            connection.action(event.target, message)

    def register_commands(self, command_handler: CommandHandler) -> None:
        for command in self.user_commands:
            command_handler.user_command_processors[command] = self

        for command in self.admin_commands:
            command_handler.admin_command_processors[command] = self

    def handle_subcommand(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if command_data.arg_count < 1 or command_data.args[0] not in self.sub_commands:
            self.respond_to_user(connection, event, "Valid subcommands: " + ", ".join(self.sub_commands))
            return

        subcommand_handler = getattr(self, '_' + command_data.args[0] + '_handler')
        subcommand_handler(connection, event, command_data)

    def _execution_checks(self, connection: ServerConnection, event: Event, command_data: CommandData) -> bool:
        if command_data.in_pm:
            if not self.allowed_in_pm:
                self.respond_to_user(connection, event, "You can't do that in a private message.")
                return False
        else:
            channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

            flag_name = command_data.command
            if command_data.command in self.command_flag_map:
                flag_name = self.command_flag_map[command_data.command]

            if not channel_data.command_settings[flag_name]:
                self.respond_to_user(connection, event, "I'm sorry, I don't do that here.")
                return False

        return self._interaction_flag_check(connection, event, command_data)

    def _interaction_flag_check(self, connection: ServerConnection, event: Event, command_data: CommandData) -> bool:
        if not self.interaction_checks:
            return True

        from timmy.command_processors import interaction_controls

        flag_name = command_data.command
        if command_data.command in self.command_flag_map:
            flag_name = self.command_flag_map[command_data.command]

        if command_data.arg_count > 0:
            target = command_data.arg_string

            if interaction_controls.interact_with_user(target, flag_name):
                return True
            else:
                self.respond_to_user(connection, event, "I'm sorry, it's been requested that I not do that.")
                return False
        else:
            if interaction_controls.interact_with_user(command_data.issuer, command_data.command):
                return True
            else:
                self.respond_to_user(connection, event, "I'm sorry, it's been requested that I not do that.")
                return False
