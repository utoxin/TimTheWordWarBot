from irc.client import Event, ServerConnection

from timmy import db_access
from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.utilities import text_generator


class RaptorCommands(BaseCommand):
    user_commands = {"raptor"}
    sub_commands = {"adopt", "release", "details", "rename", "reset"}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        user_data = db_access.user_directory.find_user_data(command_data.issuer)

        if user_data is None or not user_data.registration_data_retrieved:
            self.respond_to_user(
                    connection, event, "I'm sorry, you must be registered before you can work with "
                                       "raptors... (Please ensure you are registered and identified with "
                                       "NickServ.)"
            )
            return

        if command_data.arg_count < 1:
            self.respond_to_user(
                    connection, event, "Raptors recently became fascinated by writing, so we created a "
                                       "raptor adoption program. They will monitor your word sprints, and "
                                       "share the details with you when asked. Don't worry, they probably "
                                       "won't eat your cat."
            )
            self.respond_to_user(connection, event, "Valid subcommands: adopt, release, details, rename, reset")
            return

        self.handle_subcommand(connection, event, command_data)

    def _adopt_handler(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        user_data = db_access.user_directory.find_user_data(command_data.issuer)

        if user_data.raptor_adopted:
            self.respond_to_user(connection, event, "Due to safety regulations, you may only adopt a single raptor.")
            return

        if command_data.arg_count < 2:
            self.respond_to_user(connection, event, "You need to provide a name to adopt your new raptor.")
            return

        user_data.raptor_adopted = True
        user_data.raptor_name = " ".join(command_data.args[1:])
        user_data.raptor_favorite_color = text_generator.get_string("[color]")

        user_data.save()

        self.respond_to_user(
                connection, event,
                "Excellent! I've noted your raptor's name. Just so you know, their favorite color is {}.".format(
                        user_data.raptor_favorite_color
                )
        )
        return

    def _release_handler(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        user_data = db_access.user_directory.find_user_data(command_data.issuer)

        if not user_data.raptor_adopted:
            self.respond_to_user(
                    connection, event,
                    "I don't have any record of you having a raptor. Are you sure that isn't your cat?"
            )
            return

        self.respond_to_user(
                connection, event,
                "I'll release {} back into the wild. I'm sure they'll adjust well...".format(user_data.raptor_name)
        )

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

    def _details_handler(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        user_data = db_access.user_directory.find_user_data(command_data.issuer)

        if not user_data.raptor_adopted:
            self.respond_to_user(
                    connection, event,
                    "You haven't adopted a raptor, so they can't very well give you any details."
            )
            return

        wpm = 0

        if user_data.total_sprint_duration > 0:
            wpm = user_data.total_sprint_wordcount / user_data.total_sprint_duration

        response = "{} goes through their records. According to those records, you have written {:n} words in {:n} " \
                   "sprints, totalling {:n} minutes of writing. That's an average of {:n} words per minute. In their " \
                   "efforts to help you, {} has stolen {:n} plot bunnies from other channels.".format(
                user_data.raptor_name,
                user_data.total_sprint_wordcount,
                user_data.total_sprints,
                user_data.total_sprint_duration,
                wpm,
                user_data.raptor_name,
                user_data.raptor_bunnies_stolen
        )

        self.respond_to_user(connection, event, response)
        return

    def _rename_handler(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        user_data = db_access.user_directory.find_user_data(command_data.issuer)

        if not user_data.raptor_adopted:
            self.respond_to_user(
                    connection, event,
                    "I don't have any record of you having a raptor. I can't rename your children."
            )
            return

        if command_data.arg_count < 2:
            self.respond_to_user(
                    connection, event,
                    "You need to provide a new name."
            )
            return

        user_data.raptor_name = " ".join(command_data.args[1:])
        user_data.save()

        self.respond_to_user(
                connection, event,
                "Okay... your raptor should respond to the name {} now. Don't do this too often or you'll confuse the "
                "poor thing.".format(" ".join(command_data.args[1:]))
        )
        return

    def _reset_handler(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if command_data.arg_count < 2 or command_data.args[1] not in ['stats', 'bunnies', 'color']:
            self.respond_to_user(
                    connection, event, "Specify whether you wish to reset 'stats', 'bunnies', or favorite "
                                       "'color'"
            )
            self.respond_to_user(connection, event, "Example: !raptor reset stats")
            return

        user_data = db_access.user_directory.find_user_data(command_data.issuer)

        if command_data.args[1] == 'stats':
            self.respond_to_user(
                    connection, event, "{} opens a fresh {} notebook, and prepares to record more "
                                       "stats.".format(
                            user_data.raptor_name,
                            user_data.raptor_favorite_color
                    )
            )
            user_data.total_sprint_wordcount = 0
            user_data.total_sprints = 0
            user_data.total_sprint_duration = 0
            user_data.recorded_wars = {}

        if command_data.args[1] == 'bunnies':
            self.respond_to_user(
                    connection, event, "{} opens up the pens where they were holding {:n} plot bunnies "
                                       "and lets them run free.".format(
                            user_data.raptor_name,
                            user_data.raptor_bunnies_stolen
                    )
            )
            user_data.raptor_bunnies_stolen = 0
            user_data.last_bunny_raid = None

        if command_data.args[1] == 'color':
            user_data.raptor_favorite_color = text_generator.get_string("[color]")
            self.respond_to_user(
                    connection, event, "{} decides they much prefer {}, and copy all of your stats into "
                                       "a new notebook.".format(
                            user_data.raptor_name,
                            user_data.raptor_favorite_color
                    )
            )

        user_data.save()
