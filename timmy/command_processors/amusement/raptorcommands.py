from timmy import db_access
from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class RaptorCommands(BaseCommand):
    user_commands = ["raptor"]
    sub_commands = ["adopt", "release", "details", "rename", "reset"]

    help_topics = [('user', 'raptor commands', '!raptor adopt <name>', 'Adopts a new raptor, and names it.'),
                   ('user', 'raptor commands', '!raptor rename <name>', 'Renames your raptor.'),
                   ('user', 'raptor commands', '!raptor release', 'Releases your raptor back into the wild. (Resets '
                                                                  'all your stats.'),
                   ('user', 'raptor commands', '!raptor details', 'See the stats your raptor has kept track of.'),
                   ('user', 'raptor commands', '!war report <wordcount>', 'Use after a word war to have your raptor '
                                                                          'record your wordcount stats.'),
                   ('user', 'raptor commands', '!raptor reset <stats/bunnies/color>', 'Reset individual parts of your '
                                                                                      'raptor\'s details.')]

    def process(self, command_data: CommandData) -> None:
        user_data = db_access.user_directory.find_user_data(command_data.issuer)

        if user_data is None or not user_data.registration_data_retrieved:
            self.respond_to_user(command_data, "I'm sorry, you must be registered before you can work with raptors... "
                                               "(Please ensure you are registered and identified with NickServ.)")
            return

        if command_data.arg_count < 1:
            self.respond_to_user(command_data, "Raptors recently became fascinated by writing, so we created a raptor "
                                               "adoption program. They will monitor your word sprints, and share the "
                                               "details with you when asked. Don't worry, they probably won't eat your "
                                               "cat.")
            self.respond_to_user(command_data, "Valid subcommands: adopt, release, details, rename, reset")
            return

        self.handle_subcommand(command_data)

    def _adopt_handler(self, command_data: CommandData) -> None:
        user_data = db_access.user_directory.find_user_data(command_data.issuer)

        if user_data.raptor_adopted:
            self.respond_to_user(command_data, "Due to safety regulations, you may only adopt a single raptor.")
            return

        if command_data.arg_count < 2:
            self.respond_to_user(command_data, "You need to provide a name to adopt your new raptor.")
            return

        user_data.raptor_adopted = True
        user_data.raptor_name = " ".join(command_data.args[1:])
        user_data.raptor_favorite_color = text_generator.get_string("[color]")

        user_data.save()

        self.respond_to_user(command_data, f"Excellent! I've noted your raptor's name. Just so you know, their favorite"
                                           f" color is {user_data.raptor_favorite_color}.")
        return

    def _release_handler(self, command_data: CommandData) -> None:
        user_data = db_access.user_directory.find_user_data(command_data.issuer)

        if not user_data.raptor_adopted:
            self.respond_to_user(command_data, "I don't have any record of you having a raptor. Are you sure that isn't"
                                               " your cat?")
            return

        self.respond_to_user(command_data, f"I'll release {user_data.raptor_name} back into the wild. I'm sure they'll "
                                           f"adjust well...")

        user_data.raptor_adopted = False
        user_data.total_sprint_wordcount = 0
        user_data.total_sprints = 0
        user_data.total_sprint_duration = 0
        user_data.raptor_adopted = False
        user_data.raptor_name = ""
        user_data.raptor_favorite_color = ""
        user_data.raptor_bunnies_stolen = 0
        user_data.last_bunny_raid = None
        user_data.recorded_wars = {}

        user_data.save()

        return

    def _details_handler(self, command_data: CommandData) -> None:
        user_data = db_access.user_directory.find_user_data(command_data.issuer)

        if not user_data.raptor_adopted:
            self.respond_to_user(command_data, "You haven't adopted a raptor, so they can't very well give you any "
                                               "details.")
            return

        wpm = 0

        if user_data.total_sprint_duration > 0:
            wpm = user_data.total_sprint_wordcount / user_data.total_sprint_duration

        response = f"{user_data.raptor_name} goes through their records. According to those records, you have written" \
                   f" {user_data.total_sprint_wordcount:n} words in {user_data.total_sprints:n} sprints, totalling " \
                   f"{user_data.total_sprint_duration:n} minutes of writing. That's an average of {wpm:n} words per " \
                   f"minute. In their efforts to help you, {user_data.raptor_name} has stolen " \
                   f"{user_data.raptor_bunnies_stolen:n} plot bunnies from other channels."

        self.respond_to_user(command_data, response)
        return

    def _rename_handler(self, command_data: CommandData) -> None:
        user_data = db_access.user_directory.find_user_data(command_data.issuer)

        if not user_data.raptor_adopted:
            self.respond_to_user(command_data, "I don't have any record of you having a raptor. I can't rename your "
                                               "children.")
            return

        if command_data.arg_count < 2:
            self.respond_to_user(command_data, "You need to provide a new name.")
            return

        user_data.raptor_name = " ".join(command_data.args[1:])
        user_data.save()

        self.respond_to_user(command_data, f"Okay... your raptor should respond to the name {user_data.raptor_name} "
                                           f"now. Don't do this too often or you'll confuse the poor thing.")
        return

    def _reset_handler(self, command_data: CommandData) -> None:
        if command_data.arg_count < 2 or command_data.args[1] not in ['stats', 'bunnies', 'color']:
            self.respond_to_user(command_data, "Specify whether you wish to reset 'stats', 'bunnies', or favorite "
                                               "'color'")
            self.respond_to_user(command_data, "Example: !raptor reset stats")
            return

        user_data = db_access.user_directory.find_user_data(command_data.issuer)

        if command_data.args[1] == 'stats':
            self.respond_to_user(command_data, f"{user_data.raptor_name} opens a fresh "
                                               f"{user_data.raptor_favorite_color} notebook, and prepares to record "
                                               f"more stats.")
            user_data.total_sprint_wordcount = 0
            user_data.total_sprints = 0
            user_data.total_sprint_duration = 0
            user_data.recorded_wars = {}

        if command_data.args[1] == 'bunnies':
            self.respond_to_user(command_data, f"{user_data.raptor_name} opens up the pens where they were holding "
                                               f"{user_data.raptor_bunnies_stolen:n} plot bunnies and lets them run "
                                               f"free.")
            user_data.raptor_bunnies_stolen = 0
            user_data.last_bunny_raid = None

        if command_data.args[1] == 'color':
            user_data.raptor_favorite_color = text_generator.get_string("[color]")
            self.respond_to_user(command_data, f"{user_data.raptor_name} decides they much prefer "
                                               f"{user_data.raptor_favorite_color}, and copy all of your stats into a "
                                               f"new notebook.")

        user_data.save()
