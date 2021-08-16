import dice
from irc.client import ServerConnection, Event

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData


class DiceCommand(BaseCommand):
    user_commands = {'roll'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if not command_data.in_pm:
            channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

            if not channel_data.command_settings['dice']:
                self.respond_to_user(connection, event, "I'm sorry, I don't do that here.")
                return

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

                self.respond_to_user(connection, event, f"Your result was {dice_total}. {detailed_dice}")
            except dice.DiceBaseException as e:
                self.respond_to_user(connection, event, e.pretty_print())
        else:
            self.respond_to_user(connection, event, "Usage: !roll <dice string>")
            self.respond_to_user(connection, event, "Example: !roll 2d6")

        return
