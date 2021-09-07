import random

from irc.client import Event, ServerConnection

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class AttackCommand(BaseCommand):
    user_commands = {'attack'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        from timmy.command_processors import interaction_controls

        if command_data.in_pm:
            self.respond_to_user(connection, event, "You can't do that in a private message.")
            return

        channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

        if not channel_data.command_settings['attack']:
            self.respond_to_user(connection, event, "I'm sorry, I don't do that here.")
            return

        if command_data.arg_count > 0:
            target = command_data.arg_string

            if interaction_controls.interact_with_user(target, command_data.command):
                self.attack_command(connection, event, command_data, target)
            else:
                self.respond_to_user(connection, event, "I'm sorry, it's been requested that I not do that.")
        else:
            if interaction_controls.interact_with_user(command_data.issuer, command_data.command):
                self.attack_command(connection, event, command_data, command_data.issuer)
            else:
                self.respond_to_user(connection, event, "I'm sorry, it's been requested that I not do that.")

    def attack_command(
            self, connection: ServerConnection, event: Event, command_data: CommandData, target: str
    ) -> None:
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

        self.send_action(connection, event, attack_message)
