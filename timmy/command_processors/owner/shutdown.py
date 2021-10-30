import sys
import time

import schedule

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class ShutdownCommand(BaseCommand):
    admin_commands = ['shutdown']

    help_topics = [('admin', 'owner commands', '$shutdown', 'Shut down the bot.')]

    def process(self, command_data: CommandData):
        from timmy.core import bot_instance

        if command_data.issuer_data.global_admin:
            schedule.clear()

            from timmy.event_handlers.postauth_handler import interval
            interval.stop()

            self.respond_to_user(command_data, "Shutting down.........")
            bot_instance.connection.quit("Help, help! I'm being repressed!")
            time.sleep(1)
            sys.exit(0)
        else:
            self.respond_to_user(command_data, "Only the bot owner can do that!")
