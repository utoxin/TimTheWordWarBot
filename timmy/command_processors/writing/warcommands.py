import re
import time
from typing import Optional

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.data.war_state import WarState
from timmy.data.word_war import WordWar
from timmy.db_access import user_directory, word_war_db


class WarCommands(BaseCommand):
    user_commands = {"war", "endwar", "starwar", "startwar", "chainwar", "listall", "listwars", "joinwar", "leavewar"}
    sub_commands = {"start", "cancel", "join", "leave", "report", "list"}

    help_topics = [('user', 'word war commands', '!war start', 'Creates a new war.'),
                   ('user', 'word war commands', '!war cancel', 'Cancels an existing ware.'),
                   ('user', 'word war commands', '!war join [<war id>]', 'Signs up for notifications about a war. '
                                                                         'Defaults to most recently created.'),
                   ('user', 'word war commands', '!war leave [<war id>]', 'Cancel notifications for a war. Defaults '
                                                                          'to most recently created.'),
                   ('user', 'word war commands', '!war report', 'Report your wordcount for a war.'),
                   ('user', 'word war commands', '!war list [all]', 'List wars in current channel or globally.')]

    def process(self, command_data: CommandData) -> None:
        if command_data.command == 'endwar':
            self.respond_to_user(command_data, "The syntax for war-related commands has changed. Try '!war cancel' in "
                                               "the future.")
            command_data.command = 'war'
            command_data.args.insert(0, 'cancel')
            command_data.arg_count += 1
            command_data.arg_string = 'cancel ' + command_data.arg_string
        elif command_data.command == 'starwar':
            self.respond_to_user(command_data, "A long time ago, in a novel far, far away...")
            return
        elif command_data.command == 'startwar' or command_data.command == 'chainwar':
            self.respond_to_user(command_data, "The syntax for war-related commands has changed. Use '!war start'")
            return
        elif command_data.command == 'listall':
            self.respond_to_user(command_data, "The syntax for war-related commands has changed. Try '!war list all' in"
                                               " the future.")
            command_data.command = 'war'
            command_data.args = ['list', 'all']
            command_data.arg_count = 2
            command_data.arg_string = 'list all'
        elif command_data.command == 'listwars':
            self.respond_to_user(command_data, "The syntax for war-related commands has changed. Try '!war list' in the"
                                               " future.")
            command_data.command = 'war'
            command_data.args = ['list']
            command_data.arg_count = 1
            command_data.arg_string = 'list'
        elif command_data.command == 'joinwar':
            self.respond_to_user(command_data, "The syntax for war-related commands has changed. Try '!war join' in the"
                                               " future.")
            command_data.command = 'war'
            command_data.args.insert(0, 'join')
            command_data.arg_count += 1
            command_data.arg_string = 'join ' + command_data.arg_string
        elif command_data.command == 'leavewar':
            self.respond_to_user(command_data, "The syntax for war-related commands has changed. Try '!war leave' in "
                                               "the future.")
            command_data.command = 'war'
            command_data.args.insert(0, 'leave')
            command_data.arg_count += 1
            command_data.arg_string = 'leave ' + command_data.arg_string

        if command_data.arg_count >= 1:
            if command_data.args[0] == 'create':
                command_data.args[0] = 'start'

            if command_data.args[0] == 'end':
                command_data.args[0] = 'cancel'

        self.handle_subcommand(command_data)

    def _start_handler(self, command_data: CommandData) -> None:
        if command_data.arg_count >= 2:
            to_start = 60
            total_chains = 1
            do_randomness = False
            war_name = ""

            try:
                duration = float(command_data.args[1]) * 60
            except TypeError:
                self.respond_to_user(command_data, "I couldn't understand the duration. Was it numeric?")
                return

            if duration < 60:
                self.respond_to_user(command_data, "Duration must be at least 1 minute.")
                return

            delay = duration / 2

            # Options for all wars
            start_delay_pattern = re.compile("^(?:start|delay):([0-9.]*)$", re.IGNORECASE)

            # Options for chainwars
            chain_pattern = re.compile("^chains?:([0-9.]*)$", re.IGNORECASE)
            break_pattern = re.compile("^break:([0-9.]*)$", re.IGNORECASE)
            randomness_pattern = re.compile("^random:([01]*)$", re.IGNORECASE)

            if command_data.arg_count >= 3:
                i = 2
                while i < command_data.arg_count:
                    match = start_delay_pattern.match(command_data.args[i])
                    if match:
                        results = match.groups()
                        input_string = ""
                        try:
                            if results[0] == "" and i < (command_data.arg_count - 1):
                                i += 1
                                input_string = command_data.args[i]
                            else:
                                input_string = results[0]

                            to_start = int(float(input_string) * 60)
                        except TypeError:
                            self.respond_to_user(command_data, "I didn't understand the start delay. Was it numeric?")
                            from timmy.utilities import irc_logger
                            irc_logger.log_message(
                                    f"Word War Exception: Start delay parse error. Input was: "
                                    f"{input_string}"
                            )
                            return

                        i += 1
                        continue

                    match = chain_pattern.match(command_data.args[i])
                    if match:
                        results = match.groups()
                        input_string = ""
                        try:
                            if results[0] == "" and i < (command_data.arg_count - 1):
                                i += 1
                                input_string = command_data.args[i]
                            else:
                                input_string = results[0]

                            total_chains = int(input_string)
                        except TypeError:
                            self.respond_to_user(command_data, "I didn't understand the chain count. Was it numeric?")
                            from timmy.utilities import irc_logger
                            irc_logger.log_message(
                                    f"Word War Exception: Chain count parse error. Input was: "
                                    f"{input_string}"
                            )
                            return

                        i += 1
                        continue

                    match = break_pattern.match(command_data.args[i])
                    if match:
                        results = match.groups()
                        input_string = ""
                        try:
                            if results[0] == "" and i < (command_data.arg_count - 1):
                                i += 1
                                input_string = command_data.args[i]
                            else:
                                input_string = results[0]

                            delay = int(float(input_string) * 60)
                        except TypeError:
                            self.respond_to_user(command_data, "I didn't understand the break duration. Was it "
                                                               "numeric?")
                            from timmy.utilities import irc_logger
                            irc_logger.log_message(
                                    f"Word War Exception: Break duration parse error. Input was: "
                                    f"{input_string}"
                            )
                            return

                        i += 1
                        continue

                    match = randomness_pattern.match(command_data.args[i])
                    if match:
                        results = match.groups()
                        input_string = ""
                        try:
                            if results[0] == "" and i < (command_data.arg_count - 1):
                                i += 1
                                input_string = command_data.args[i]
                            else:
                                input_string = results[0]

                            do_randomness = int(input_string) == 1

                        except TypeError:
                            self.respond_to_user(command_data, "I didn't understand the randomness flag. Was it "
                                                               "numeric?")

                            from timmy.utilities import irc_logger
                            irc_logger.log_message(
                                    f"Word War Exception: Random flag parse error. Input was: "
                                    f"{input_string}"
                            )
                            return

                        i += 1
                        continue

                    if war_name == "":
                        war_name = command_data.args[i]
                    else:
                        war_name += " " + command_data.args[i]

                    i += 1

            if war_name == "":
                war_name = command_data.issuer + "'s War"

            if re.match("^\\d+$", war_name):
                self.respond_to_user(command_data, f"War names must be more than a number. It's possible you meant to "
                                                   f"specify the start delay. Try: !war start {command_data.args[1]} "
                                                   f"delay:{war_name}")
                return

            current_epoch = time.time()

            war = WordWar()

            if total_chains <= 1:
                war.basic_setup(command_data.channel, command_data.issuer, war_name, duration, current_epoch + to_start)
            else:
                war.advanced_setup(
                        command_data.channel, command_data.issuer, war_name, duration,
                        current_epoch + to_start, total_chains, delay, do_randomness
                )

            core.war_ticker.active_wars.add(war)

            if command_data.channel in core.bot_instance.channels:
                core.bot_instance.channels[command_data.channel].newest_war_id = war.get_id()

            if to_start > 0:
                self.respond_to_user(command_data, "Your word war, {}, will start in {:.1f} minutes. The ID is "
                                                   "{:d}-{:d}."
                                     .format(war.get_name(), to_start / 60, war.year, war.war_id))
        else:
            self.respond_to_user(command_data, "Usage: !war start <duration in minutes> [<options>] [<name>]")
            self.respond_to_user(command_data, "Options for all wars: delay:<minutes>")
            self.respond_to_user(command_data, "Options for chain wars: chains:<chain count>, random:1, "
                                               "break:<minutes>")
            self.respond_to_user(command_data, "Example: !war start 10 delay:5 Let's Write!")

    def _cancel_handler(self, command_data: CommandData) -> None:
        if command_data.arg_count > 1:
            name = " ".join(command_data.args[1:])

            war_by_name = self._find_war_by_name(name, command_data.channel)
            war_by_id = self._find_war_by_id(name)

            if war_by_name:
                self._remove_war(command_data, war_by_name)
            elif war_by_id:
                self._remove_war(command_data, war_by_id)
            else:
                self.respond_to_user(command_data, "I don't know of a war with the name or ID '{}'.".format(name))
        else:
            self.respond_to_user(command_data, "Syntax: !war cancel <name or id>")

    def _join_handler(self, command_data: CommandData) -> None:
        war = self._find_join_leave_war(command_data)

        if war is not None:
            if war.state == WarState.PENDING or war.state == WarState.ACTIVE:
                joiner = command_data.issuer_data.authed_user or command_data.issuer

                war.add_member(joiner)

                self.respond_to_user(command_data, "You have joined the war.")
            else:
                self.respond_to_user(command_data, "That war is over or cancelled. Sorry!")

    def _report_handler(self, command_data: CommandData) -> None:
        if command_data.arg_count < 2:
            self.respond_to_user(command_data, "Usage: !war report <wordcount> [<war id>]")
            self.respond_to_user(command_data, "Note: Wordcount should be words written during the war, not total "
                                               "count. The war id is optional. The last completed war will be selected "
                                               "by default.")
        else:
            user_data = user_directory.find_user_data(command_data.issuer)
            channel_data = core.bot_instance.channels[command_data.channel]

            if len(command_data.args) == 3:
                war: WordWar = word_war_db.load_war_by_id(command_data.args[2])
            elif channel_data.last_war_id != "":
                war: WordWar = word_war_db.load_war_by_id(channel_data.last_war_id)
            else:
                self.respond_to_user(command_data, "I don't know which war finished last. Try providing the War ID")
                return

            if user_data is None or not user_data.raptor_adopted:
                self.respond_to_user(command_data, "I'm sorry, you must be a registered user and have an adopted raptor"
                                                   " to record your stats... (!raptor command)")
            elif war is None:
                self.respond_to_user(command_data, "That war couldn't be found...")
            elif war.state is WarState.CANCELLED:
                self.respond_to_user(command_data, "That war was cancelled. Sorry!")
            elif (war.total_chains == 1 and war.state is not WarState.FINISHED) or (war.total_chains > 1 and
                                                                                    war.current_chain == 1):
                self.respond_to_user(command_data, "Can't record an entry for a war that isn't complete. Sorry!")
            else:
                try:
                    wordcount = int(float(command_data.args[1]))
                except TypeError:
                    self.respond_to_user(command_data, "I didn't understand the wordcount. Was it numeric?")
                    from timmy.utilities import irc_logger
                    irc_logger.log_message(
                            f"Word War Exception: Wordcount parse error. Input was: "
                            f"{command_data.args[1]}"
                    )
                    return

                if war.get_chain_id() in user_data.recorded_wars:
                    user_data.total_sprint_wordcount -= user_data.recorded_wars[war.get_chain_id()]
                else:
                    user_data.total_sprints += 1
                    user_data.total_sprint_duration += war.duration() / 60

                user_data.total_sprint_wordcount += wordcount

                user_data.recorded_wars[war.get_chain_id()] = wordcount

                user_data.save()

                channel_data.raptor_data['strength'] += min(10, int(war.base_duration / 600))

                self.respond_to_user(command_data, f"{user_data.raptor_name} pulls out their "
                                                   f"{user_data.raptor_favorite_color} notebook and makes a note of "
                                                   f"that wordcount.")

                if not channel_data.is_muzzled():
                    core.raptor_ticker.war_report(user_data, war, wordcount)

    def _leave_handler(self, command_data: CommandData) -> None:
        war = self._find_join_leave_war(command_data)

        if war is not None:
            leaver = command_data.issuer_data.authed_user or command_data.issuer

            war.remove_member(leaver)
            self.respond_to_user(command_data, "You have left the war.")

    def _list_handler(self, command_data: CommandData) -> None:
        all_wars = command_data.arg_count > 1 and command_data.args[1].lower() == "all"
        responded = False

        if len(core.war_ticker.active_wars) > 0:
            max_id_length = 1
            max_duration_length = 1

            for war in core.war_ticker.active_wars:
                war_id = "{:d}-{:d}".format(war.year, war.war_id)
                if len(war_id) > max_id_length:
                    max_id_length = len(war_id)

                war_duration = war.get_duration_text(war.duration())
                if len(war_duration) > max_duration_length:
                    max_duration_length = len(war_duration)

            for war in core.war_ticker.active_wars:
                if all_wars or war.channel.lower() == command_data.channel.lower():
                    if all_wars:
                        output = war.get_description_with_channel(max_id_length, max_duration_length)
                    else:
                        output = war.get_description(max_id_length, max_duration_length)

                    self.respond_to_user(command_data, output)
                    responded = True

        if not responded:
            self.respond_to_user(command_data, "No wars are currently available.")

    def _find_join_leave_war(self, command_data: CommandData) -> Optional[WordWar]:
        if command_data.in_pm:
            self.respond_to_user(command_data, "Sorry. This currently only works in regular channels.")
            return None
        else:
            channel_data = core.bot_instance.channels[command_data.channel]

        if command_data.arg_count == 1:
            if channel_data.newest_war_id != "":
                war = self._find_war_by_id(channel_data.newest_war_id)
            else:
                self.respond_to_user(command_data, "Hmmm. I don't seem to have a record of an upcoming war. Try "
                                                   "providing a war ID?")
                self.respond_to_user(command_data, "Usage: !war {} [<war id>]".format(command_data.args[0]))
                return None
        else:
            war = self._find_war_by_id(command_data.args[1])

        if war is None:
            self.respond_to_user(command_data, "That war id was not found.")

        return war

    @staticmethod
    def _find_war_by_id(search_id) -> Optional[WordWar]:
        for war in core.war_ticker.active_wars:
            war_id = "{:d}-{:d}".format(war.year, war.war_id)
            if war_id == search_id:
                return war

        return None

    @staticmethod
    def _find_war_by_name(search_name, channel) -> Optional[WordWar]:
        return_war = None

        for war in core.war_ticker.active_wars:
            if war.get_internal_name().lower() == search_name.lower() and war.channel.lower() == channel.lower():
                if return_war is None:
                    return_war = war
                else:
                    return None

        return return_war

    def _remove_war(self, command_data: CommandData, war: WordWar) -> None:
        if command_data.issuer.lower() == war.starter.lower():
            core.war_ticker.active_wars.remove(war)
            war.cancel_war()
            self.respond_to_user(command_data, "{} has been ended.".format(war.get_name()))
        else:
            self.respond_to_user(command_data, "Only the starter of a war can end it early.")
