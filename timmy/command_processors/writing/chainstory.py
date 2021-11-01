import re
from datetime import datetime

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class ChainStoryCommands(BaseCommand):
    user_commands = ["chainstory", "chainlast", "chainnew", "chaininfo", "chaincount"]
    sub_commands = ["last", "new", "info", "count"]

    interaction_checks = False

    help_topics = [('user', 'chain story', '!chain info', 'General info about the current status of my novel.'),
                   ('user', 'chain story', '!chain last', 'The last paragraph of my novel, so you have something to '
                                                          'base the next bit on.'),
                   ('user', 'chain story', '!chain new <paragraph>', 'Provide the next paragraph of my great novel!'),
                   ('user', 'chain story', '!chain count', 'Get the current wordcount and contributor count for my '
                                                           'novel.')]

    command_flag_map = {
        'chainstory': 'chainstory',
        'chainlast':  'chainstory',
        'chainnew':   'chainstory',
        'chaininfo':  'chainstory',
        'chaincount': 'chainstory'
    }

    def process(self, command_data: CommandData) -> None:
        if self._execution_checks(command_data):
            if command_data.command == 'chainlast':
                self.respond_to_user(command_data, "The syntax for chain-related commands has changed. Try '!chainstory"
                                                   " last' in the future.")
                command_data.command = 'chainstory'
                command_data.args.insert(0, 'last')
                command_data.arg_count += 1
                command_data.arg_string = 'last ' + command_data.arg_string
            elif command_data.command == 'chainnew':
                self.respond_to_user(command_data, "The syntax for chain-related commands has changed. Try '!chainstory"
                                                   " new' in the future.")
                command_data.command = 'chainstory'
                command_data.args.insert(0, 'new')
                command_data.arg_count += 1
                command_data.arg_string = 'new ' + command_data.arg_string
            elif command_data.command == 'chaininfo':
                self.respond_to_user(command_data, "The syntax for chain-related commands has changed. Try '!chainstory"
                                                   " info' in the future.")
                command_data.command = 'chainstory'
                command_data.args.insert(0, 'info')
                command_data.arg_count += 1
                command_data.arg_string = 'info ' + command_data.arg_string
            elif command_data.command == 'chaincount':
                self.respond_to_user(command_data, "The syntax for chain-related commands has changed. Try '!chainstory"
                                                   " count' in the future.")
                command_data.command = 'chainstory'
                command_data.args.insert(0, 'count')
                command_data.arg_count += 1
                command_data.arg_string = 'count ' + command_data.arg_string

            self.handle_subcommand(command_data)

    def _last_handler(self, command_data: CommandData) -> None:
        from timmy.db_access import chainstory_db
        from timmy.core import bot_instance

        last_lines = chainstory_db.get_last_lines()

        for line in last_lines:
            bot_instance.connection.privmsg(command_data.issuer, line[:480])

        self.respond_to_user(command_data, "I sent you the last three paragraphs in a private message... They're too "
                                           "awesome for everyone to see!")
        return

    def _new_handler(self, command_data: CommandData) -> None:
        month = datetime.now().month
        if month != 11:
            self.respond_to_user(command_data, "Sorry, that command won't work outside November!")
            return

        message = " ".join(command_data.args[1:])
        message = re.sub(r'^<?(.*?)>?$', r'\1', message)

        if message == "":
            self.respond_to_user(command_data, "blinks, and looks confused. \"But there's nothing there. That won't "
                                               "help my wordcount!\"")
            return

        from timmy.db_access import chainstory_db
        chainstory_db.add_line(message, command_data.issuer)

        word_count = chainstory_db.word_count()

        self.send_action(command_data, f"quickly jots down what {command_data.issuer} said. \"Thanks! My novel is now "
                                       f"{word_count:,} words long!\"")

    def _info_handler(self, command_data: CommandData) -> None:
        from timmy.db_access import chainstory_db
        from timmy.core import bot_instance

        last_lines = chainstory_db.get_last_lines()
        word_count = chainstory_db.word_count()
        author_count = chainstory_db.author_count()

        for line in last_lines:
            bot_instance.connection.privmsg(command_data.issuer, line)

        self.respond_to_user(command_data, f"My novel is currently {word_count:,} words long, with paragraphs written "
                                           f"by {author_count:,} different people, and I sent you the last three "
                                           f"paragraphs in a private message... They're too awesome for everyone to "
                                           f"see!")

        self.respond_to_user(command_data, "You can read an excerpt in my profile here: "
                                           "https://nanowrimo.org/participants/timmybot")
        return

    def _count_handler(self, command_data: CommandData) -> None:
        from timmy.db_access import chainstory_db
        word_count = chainstory_db.word_count()
        author_count = chainstory_db.author_count()

        self.respond_to_user(command_data, f"My novel is currently {word_count:,} words long, with paragraphs written "
                                           f"by {author_count:,} different people.")
        return
