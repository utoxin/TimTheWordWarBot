import dice
from irc.client import Event

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class DiceCommand(BaseCommand):
    user_commands = {'roll'}
    interaction_checks = False

    def process(self, event: Event, command_data: CommandData) -> None:
        if self._execution_checks(event, command_data):
            if command_data.arg_count > 0:
                dice_string: str = command_data.arg_string

                try:
                    dice_result = dice.roll(dice_string)

                    try:
                        dice_total = sum(dice_result)

                        if len(dice_result) <= 20:
                            detailed_dice = dice.utilities.verbose_print(dice_result)
                        else:
                            detailed_dice = "[ too many dice ]"
                    except TypeError:
                        dice_total = dice_result
                        detailed_dice = "[ no detailed dice ]"

                    self.respond_to_user(event, f"Your result was {dice_total}. {detailed_dice}")
                except dice.DiceBaseException as e:
                    self.respond_to_user(event, e.pretty_print())
            else:
                self.respond_to_user(event, "Usage: !roll <dice string>")
                self.respond_to_user(event, "Example: !roll 2d6")
