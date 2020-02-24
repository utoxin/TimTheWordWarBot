from timmy.event_handlers import CommandHandler


class BaseCommand:
    user_commands = {}
    admin_commands = {}

    @staticmethod
    def respond_to_user(connection, event, message):
        if event.type == "privmsg":
            connection.privmsg(event.source.nick, message)
        else:
            connection.privmsg(event.target, event.source.nick + ": " + message)

    def register_commands(self, command_handler: CommandHandler):
        for command in self.user_commands:
            command_handler.user_command_processors[command] = self

        for command in self.admin_commands:
            command_handler.admin_command_processors[command] = self
