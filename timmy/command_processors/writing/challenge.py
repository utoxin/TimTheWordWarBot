from irc.client import Event, ServerConnection

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class ChallengeCommands(BaseCommand):
    user_commands = {"challenge", "challengefor"}

    command_flag_map = {
        'challenge':    'challenge',
        'challengefor': 'challenge'
    }

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if self._execution_checks(connection, event, command_data):
            if command_data.command == 'challenge':
                self.challenge_command(connection, event, command_data.issuer)
            elif command_data.command == 'challengefor':
                if command_data.arg_count < 1:
                    self.respond_to_user(connection, event, f"Usage: !{command_data.command} <target>")
                    return

                self.challenge_command(connection, event, command_data.args[0])

    def challenge_command(self, connection: ServerConnection, event: Event, recipient: str) -> None:
        challenge = text_generator.get_string("[challenge_template]", {})

        self.send_action(connection, event, f"challenges {recipient}: {challenge}")
