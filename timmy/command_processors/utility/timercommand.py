import threading

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class TimerCommand(BaseCommand):
    user_commands = ['eggtimer', 'timer']

    help_topics = [('user', 'general commands', '!timer <time>', 'I will send you a message after <time> minutes.')]

    def process(self, command_data: CommandData) -> None:
        duration = 15
        if command_data.arg_count > 0:
            try:
                duration = int(command_data.args[0])
            except ValueError:
                self.respond_to_user(command_data, "Could not understand first parameter. Was it numeric?")
                return

        duration = duration * 60

        self.respond_to_user(command_data, "Your timer has been set.")

        x = threading.Timer(duration, self._timer_thread, args = [command_data])
        x.start()

    def _timer_thread(self, command_data: CommandData) -> None:
        self.respond_to_user(command_data, "Your timer has expired!")
