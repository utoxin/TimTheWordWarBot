from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class CreditsCommand(BaseCommand):
    user_commands = ['credits']

    help_topics = [('user', 'general commands', '!credits', 'Details of my creators, and where to find my source '
                                                            'code.')]

    def process(self, command_data: CommandData) -> None:
        self.respond_to_user(
            command_data, "I was created by MysteriousAges in 2008. Utoxin started helping during NaNoWriMo 2010, and "
                          "has been the primary developer since 2011. Sourcecode is available here: "
                          "https://github.com/utoxin/TimTheWordWarBot, and my NaNoWriMo profile page is here: "
                          "https://nanowrimo.org/participants/timmybot"
            )
