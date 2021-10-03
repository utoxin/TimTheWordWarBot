import random
import threading

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class SummonCommand(BaseCommand):
    user_commands = {'summon', 'banish'}
    amusement_commands = {'summon', 'banish'}

    def process(self, command_data: CommandData) -> None:
        if self._execution_checks(command_data):
            if command_data.arg_count == 0:
                target = text_generator.get_string("[deity]")
            else:
                target = command_data.arg_string

            if command_data.command == 'summon':
                self.summon_command(command_data, target)
            else:
                self.banish_command(command_data, target)

    def summon_command(self, command_data: CommandData, target: str) -> None:
        initial_delay = random.random() + 0.5
        initial_message = text_generator.get_string("[summon_start]", {
            'target': target,
            'summoner': command_data.issuer,
            'alternate': '[deity]'
        })

        x = threading.Timer(initial_delay, self._timer_thread, args = [command_data, initial_message])
        x.start()

        second_message = text_generator.get_string("[summon_end]", {
            'target':    target,
            'summoner':  command_data.issuer,
            'alternate': '[deity]'
        })
        secondary_delay = initial_delay + 0.5 + random.random() * 2
        y = threading.Timer(secondary_delay, self._timer_thread, args = [command_data, second_message])
        y.start()

    def banish_command(self, command_data: CommandData, target: str) -> None:
        initial_delay = random.random() + 0.5
        initial_message = text_generator.get_string("[banish_start]", {
            'target': target,
            'banisher': command_data.issuer,
            'alternate': '[deity]'
        })

        x = threading.Timer(initial_delay, self._timer_thread, args = [command_data, initial_message])
        x.start()

        second_message = text_generator.get_string("[banish_end]", {
            'target':    target,
            'banisher':  command_data.issuer,
            'alternate': '[deity]'
        })
        secondary_delay = initial_delay + 0.5 + random.random() * 2
        y = threading.Timer(secondary_delay, self._timer_thread, args = [command_data, second_message])
        y.start()

    def _timer_thread(self, command_data: CommandData, message: str) -> None:
        self.send_action(command_data, message)
