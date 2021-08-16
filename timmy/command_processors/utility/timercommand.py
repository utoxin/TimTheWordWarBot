import threading

from irc.client import ServerConnection, Event
from irc.dict import IRCDict

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class TimerCommand(BaseCommand):
    user_commands = {'eggtimer', 'timer'}

    def __init__(self):
        self.user_timers = IRCDict()

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        duration = 15
        if command_data.arg_count > 0:
            try:
                duration = int(command_data.args[0])
            except TypeError:
                self.respond_to_user(connection, event, "Could not understand first parameter. Was it numeric?")
                return

        duration = duration * 60

        self.respond_to_user(connection, event, "Your timer has been set.")

        x = threading.Timer(duration, self._timer_thread, args=(connection, event))
        x.start()

    def _timer_thread(self, connection: ServerConnection, event: Event) -> None:
        self.respond_to_user(connection, event, "Your timer has expired!")
