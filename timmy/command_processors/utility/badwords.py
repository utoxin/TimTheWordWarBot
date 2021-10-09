from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class BadWordCommands(BaseCommand):
    admin_commands = ['badword', 'badpair']
    help_topics = [('admin', 'core admin commands', '$badword <word>', 'Cleans the given word from Timmy\'s Markov DB'),
                   ('admin', 'core admin commands', '$badpair <word1> <word2>', 'Clean the given pair of words from '
                                                                                'Timmy\'s Markov DB')]

    def process(self, command_data: CommandData):
        if command_data.command == 'badword':
            if command_data.arg_count != 1 or command_data.args[0] == '':
                self.respond_to_user(command_data, "Usage: $badword <word>")
            else:
                from timmy.utilities import markov_processor
                markov_processor.add_bad_word(command_data.args[0])

                self.send_action(command_data, "quickly goes through his records, and purges all knowledge of that "
                                               "horrible word.")
        else:
            if command_data.arg_count != 2 or command_data.args[0] == '' or command_data.args[1] == '':
                self.respond_to_user(command_data, "Usage: $badpair <word1> <word2>")
            else:
                from timmy.utilities import markov_processor
                markov_processor.add_bad_pair(command_data.args[0], command_data.args[1])

                self.send_action(command_data, "quickly goes through his records, and purges all knowledge of that "
                                               "horrible phrase.")
