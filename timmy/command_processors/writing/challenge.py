from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class ChallengeCommands(BaseCommand):
    user_commands = ["challenge", "challengefor"]
    amusement_commands = ["challengefor"]
    amusement_requires_target = True

    command_flag_map = {
        'challenge':    'challenge',
        'challengefor': 'challenge'
    }

    help_topics = [('user', 'challenge commands', '!challenge', 'Request a challenge.'),
                   ('user', 'challenge commands', '!challengefor <name>', 'Challenge someone else.')]

    def process(self, command_data: CommandData) -> None:
        if self._execution_checks(command_data):
            if command_data.command == 'challenge':
                self.challenge_command(command_data, command_data.issuer)
            elif command_data.command == 'challengefor':
                if command_data.arg_count < 1:
                    self.respond_to_user(command_data, f"Usage: !{command_data.command} <target>")
                    return

                self.challenge_command(command_data, command_data.args[0])

    def challenge_command(self, command_data: CommandData, recipient: str) -> None:
        challenge = text_generator.get_string("[challenge_template]", {})

        self.send_action(command_data, f"challenges {recipient}: {challenge}")
