import random

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class AttackCommand(BaseCommand):
    user_commands = ['attack']

    def process(self, command_data: CommandData) -> None:
        if self._execution_checks(command_data):
            if command_data.arg_count > 0:
                target = command_data.arg_string
                self.attack_command(command_data, target)
            else:
                self.attack_command(command_data, command_data.issuer)

    def attack_command(self, command_data: CommandData, target: str) -> None:
        damage_number = random.betavariate(3, 3) * 10000

        if damage_number > 9000:
            damage = 'over 9000'
        else:
            damage = f"{damage_number:,.2f}"

        attack_message = text_generator.get_string(
                "[attack_message]", {
                    'source': command_data.issuer,
                    'target': target,
                    'damage': damage
                }
        )

        self.send_action(command_data, attack_message)
