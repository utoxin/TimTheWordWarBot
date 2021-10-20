from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class ReloadCommand(BaseCommand):
    admin_commands = ['reload']
    help_topics = [('admin', 'owner commands', '$loglevel <level>', 'Set the active log level.')]

    def process(self, command_data: CommandData):
        if command_data.issuer_data.global_admin:
            from timmy.core import idle_ticker
            idle_ticker.reset_timer()

            from timmy.core import war_ticker
            war_ticker.reset_timer()

            from timmy.utilities import markov_processor
            markov_processor.reset_timer()

            self.respond_to_user(command_data, "Timers restarted.")
        else:
            self.respond_to_user(command_data, "Only the bot owner can do that!")
