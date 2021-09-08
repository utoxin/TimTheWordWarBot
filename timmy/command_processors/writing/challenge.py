from irc.client import Event, ServerConnection

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class ChallengeCommands(BaseCommand):
    user_commands = {"challenge", "challengefor"}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        from timmy.command_processors import interaction_controls

        if not command_data.in_pm:
            channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

            if not channel_data.command_settings['challenge']:
                self.respond_to_user(connection, event, "I'm sorry, I don't do that here.")
                return

        if command_data.command == 'challengefor':
            if command_data.arg_count < 1:
                self.respond_to_user(connection, event, f"Usage: !{command_data.command} <target>")
                return
            else:
                target = command_data.args[0]

                if not interaction_controls.interact_with_user(target, 'challenge'):
                    self.respond_to_user(connection, event, "I'm sorry, it's been requested that I not do that.")
                    return

        elif not interaction_controls.interact_with_user(command_data.issuer, 'challenge'):
            self.respond_to_user(connection, event, "I'm sorry, it's been requested that I not do that.")
            return

        if command_data.command == 'challenge':
            self.challenge_command(connection, event, command_data.issuer)
        elif command_data.command == 'challengefor':
            self.challenge_command(connection, event, command_data.args[0])

    def challenge_command(self, connection: ServerConnection, event: Event, recipient: str) -> None:
        challenge = text_generator.get_string("[challenge_template]", {})

        self.send_action(connection, event, f"challenges {recipient}: {challenge}")
