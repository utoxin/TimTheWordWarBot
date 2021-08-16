from irc.client import Event, ServerConnection

from timmy.data.command_data import CommandData
from timmy.event_handlers import CommandHandler


class BaseCommand:
    user_commands = {}
    admin_commands = {}
    sub_commands = {}

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
