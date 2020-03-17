import re
import time

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.core import bot_instance, raptor_ticker
from timmy.data.command_data import CommandData
from timmy.data.war_state import WarState
from timmy.data.word_war import WordWar
from timmy.db_access import user_directory, word_war_db


class WarCommands(BaseCommand):
    user_commands = {"war", "endwar", "starwar", "startwar", "chainwar", "listall", "listwars", "joinwar", "leavewar"}
    sub_commands = {"start", "cancel", "join", "leave", "report", "list"}

    def process(self, connection, event, command_data: CommandData):
        if command_data.command == 'endwar':
            self.respond_to_user(connection, event, "The syntax for war-related commands has changed. Try "
                                                    "'!war cancel' in the future.")
            command_data.command = 'war'
            command_data.args.insert(0, 'cancel')
            command_data.arg_count += 1
            command_data.arg_string = 'cancel ' + command_data.arg_string
        elif command_data.command == 'starwar':
            self.respond_to_user(connection, event, "A long time ago, in a novel far, far away...")
            return
        elif command_data.command == 'startwar' or command_data.command == 'chainwar':
            self.respond_to_user(connection, event, "The syntax for war-related commands has changed. Use '!war start'")
            return
        elif command_data.command == 'listall':
            self.respond_to_user(connection, event, "The syntax for war-related commands has changed. Try "
                                                    "'!war list all' in the future.")
            command_data.command = 'war'
            command_data.args = ['list', 'all']
            command_data.arg_count = 2
            command_data.arg_string = 'list all'
        elif command_data.command == 'listwars':
            self.respond_to_user(connection, event, "The syntax for war-related commands has changed. Try "
                                                    "'!war list' in the future.")
            command_data.command = 'war'
            command_data.args = ['list']
            command_data.arg_count = 1
            command_data.arg_string = 'list'
        elif command_data.command == 'joinwar':
            self.respond_to_user(connection, event, "The syntax for war-related commands has changed. Try "
                                                    "'!war join' in the future.")
            command_data.command = 'war'
            command_data.args.insert(0, 'join')
            command_data.arg_count += 1
            command_data.arg_string = 'join ' + command_data.arg_string
        elif command_data.command == 'leavewar':
            self.respond_to_user(connection, event, "The syntax for war-related commands has changed. Try "
                                                    "'!war leave' in the future.")
            command_data.command = 'war'
            command_data.args.insert(0, 'leave')
            command_data.arg_count += 1
            command_data.arg_string = 'leave ' + command_data.arg_string

        if command_data.arg_count >= 1:
            if command_data.args[0] == 'create':
                command_data.args[0] = 'start'

            if command_data.args[0] == 'end':
                command_data.args[0] = 'cancel'

        self.handle_subcommand(connection, event, command_data)

    def _start_handler(self, connection, event, command_data: CommandData):
        if command_data.arg_count >= 2:
            to_start = 60
            total_chains = 1
            do_randomness = False
            war_name = ""

            try:
                duration = float(command_data.args[1]) * 60
            except TypeError:
                self.respond_to_user(connection, event, "I couldn't understand the duration. Was it numeric?")
                return

            if duration < 60:
                self.respond_to_user(connection, event, "Duration must be at least 1 minute.")
                return

            delay = duration / 2

            # Options for all wars
            start_delay_pattern = re.compile("^(?:start|delay):([0-9.]*)$", re.IGNORECASE)

            # Options for chainwars
            chain_pattern = re.compile("^chain(?:s?):([0-9.]*)$", re.IGNORECASE)
            break_pattern = re.compile("^break:([0-9.]*)$", re.IGNORECASE)
            randomness_pattern = re.compile("^random:([01]*)$", re.IGNORECASE)

            if command_data.arg_count >= 3:
                i = 2
                while i < command_data.arg_count:
                    match = start_delay_pattern.match(command_data.args[i])
                    if match:
                        results = match.groups()
                        try:
                            if results[0] == "" and i < (command_data.arg_count - 1):
                                i += 1
                                to_start = int(float(command_data.args[i]) * 60)
                            else:
                                to_start = int(float(results[0]) * 60)
                        except TypeError:
                            self.respond_to_user(connection, event,
                                                 "I didn't understand the start delay. Was it numeric?")
                            # TODO: Exception Logging
                            return

                        i += 1
                        continue

                    match = chain_pattern.match(command_data.args[i])
                    if match:
                        results = match.groups()
                        try:
                            if results[0] == "" and i < (command_data.arg_count - 1):
                                i += 1
                                total_chains = int(command_data.args[i])
                            else:
                                total_chains = int(results[0])
                        except TypeError:
                            self.respond_to_user(connection, event,
                                                 "I didn't understand the chain count. Was it numeric?")
                            # TODO: Exception Logging
                            return

                        i += 1
                        continue

                    match = break_pattern.match(command_data.args[i])
                    if match:
                        results = match.groups()
                        try:
                            if results[0] == "" and i < (command_data.arg_count - 1):
                                i += 1
                                delay = int(float(command_data.args[i]) * 60)
                            else:
                                delay = int(float(results[0]) * 60)
                        except TypeError:
                            self.respond_to_user(connection, event,
                                                 "I didn't understand the break duration. Was it numeric?")
                            # TODO: Exception Logging
                            return

                        i += 1
                        continue

                    match = randomness_pattern.match(command_data.args[i])
                    if match:
                        results = match.groups()
                        try:
                            if results[0] == "" and i < (command_data.arg_count - 1):
                                i += 1
                                do_randomness = int(command_data.args[i]) == 1
                            else:
                                do_randomness = int(results[0]) == 1
                        except TypeError:
                            self.respond_to_user(connection, event,
                                                 "I didn't understand the randomness flag. Was it numeric?")
                            # TODO: Exception Logging
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
                self.respond_to_user(connection, event, "War names must be more than a number. It's possible you "
                                                        "meant to specify the start delay. Try: !war start {} "
                                                        "delay:{}".format(command_data.args[1], war_name))
                return

            current_epoch = time.time()

            war = WordWar()

            if total_chains <= 1:
                war.basic_setup(command_data.channel, event.source.nick, war_name, duration, current_epoch + to_start)
            else:
                war.advanced_setup(command_data.channel, event.source.nick, war_name, duration,
                                   current_epoch + to_start, total_chains, delay, do_randomness)

            core.war_ticker_instance.wars.append(war)

            if command_data.channel in bot_instance.channels:
                bot_instance.channels[command_data.channel].newest_war_id = war.get_id()

            if to_start > 0:
                self.respond_to_user(connection, event,
                                     "Your word war, {}, will start in {:.1f} minutes. The ID is {:d}-{:d}.".format(
                                             war.get_name(), to_start / 60, war.year, war.war_id))
        else:
            self.respond_to_user(connection, event, "Usage: !war start <duration in minutes> [<options>] [<name>]")
            self.respond_to_user(connection, event, "Options for all wars: delay:<minutes>")
            self.respond_to_user(connection, event,
                                 "Options for chain wars: chains:<chain count>, random:1, break:<minutes>")
            self.respond_to_user(connection, event, "Example: !war start 10 delay:5 Let's Write!")

    def _cancel_handler(self, connection, event, command_data: CommandData):
        if command_data.arg_count > 1:
            name = " ".join(command_data.args[1:])

            war_by_name = self._find_war_by_name(name, command_data.channel)
            war_by_id = self._find_war_by_id(name)

            if war_by_name:
                self._remove_war(connection, event, command_data, war_by_name)
            elif war_by_id:
                self._remove_war(connection, event, command_data, war_by_id)
            else:
                self.respond_to_user(connection, event, "I don't know of a war with the name or ID '{}'.".format(name))
        else:
            self.respond_to_user(connection, event, "Syntax: !war cancel <name or id>")

    def _join_handler(self, connection, event, command_data: CommandData):
        war = self._find_join_leave_war(connection, event, command_data)

        if war is not None:
            if war.state == WarState.PENDING or war.state == WarState.ACTIVE:
                war.add_member(command_data.issuer)
                self.respond_to_user(connection, event, "You have joined the war.")
            else:
                self.respond_to_user(connection, event, "That war is over or cancelled. Sorry!")

    def _report_handler(self, connection, event, command_data: CommandData):
        if command_data.arg_count < 2:
            self.respond_to_user(connection, event, "Usage: !war report <wordcount> [<war id>]")
            self.respond_to_user(connection, event, "Note: Wordcount should be words written during the war, not total "
                                                    "count. The war id is optional. The last completed war will be "
                                                    "selected by default.")
        else:
            user_data = user_directory.find_user_data(command_data.issuer)
            channel_data = bot_instance.channels[command_data.channel]

            if len(command_data.args) == 3:
                war = word_war_db.load_war_by_id(command_data.args[2])
            elif channel_data.last_war_id != "":
                war = word_war_db.load_war_by_id(channel_data.last_war_id)
            else:
                self.respond_to_user(connection, event, "I don't know which war finished last. Try providing the War "
                                                        "ID")
                return

            if user_data is None or not user_data.raptor_adopted:
                self.respond_to_user(connection, event, "I'm sorry, you must be a registered user and have an adopted "
                                                        "raptor to record your stats... (!raptor command)")
            elif war is None:
                self.respond_to_user(connection, event, "That war couldn't be found...")
            elif war.war_state is WarState.CANCELLED:
                self.respond_to_user(connection, event, "That war was cancelled. Sorry!")
            elif (war.total_chains == 1 and war.war_state is not WarState.FINISHED) or (war.total_chains > 1 and
                                                                                        war.current_chain == 1):
                self.respond_to_user(connection, event, "Can't record an entry for a war that isn't complete. Sorry!")
            else:
                try:
                    wordcount = int(float(command_data.args[1]))
                except TypeError:
                    self.respond_to_user(connection, event,
                                         "I didn't understand the wordcount. Was it numeric?")
                    # TODO: Exception Logging
                    return

                if war.get_chain_id() in user_data.recorded_wars:
                    user_data.total_sprint_wordcount -= user_data.recorded_wars[war.get_chain_id()]
                else:
                    user_data.total_sprints += 1
                    user_data.total_sprint_duration += war.duration / 60

                user_data.total_sprint_wordcount += wordcount
                user_data.recorded_wars[war.get_chain_id()] = wordcount

                user_data.save()

                channel_data.raptor['strength'] += min(10, war.base_duration / 600)

                self.respond_to_user(connection, event, "{} pulls out their {} notebook and makes a note of that "
                                                        "wordcount.".format(user_data.raptor_name,
                                                                            user_data.raptor_favorite_color))

                if not channel_data.is_muzzled():
                    raptor_ticker.handle_war_report(user_data, war, wordcount)

    def _leave_handler(self, connection, event, command_data: CommandData):
        war = self._find_join_leave_war(connection, event, command_data)

        if war is not None:
            war.remove_member(command_data.issuer)
            self.respond_to_user(connection, event, "You have left the war.")

    def _list_handler(self, connection, event, command_data: CommandData):
        all_wars = command_data.arg_count > 1 and command_data.args[1].lower() == "all"
        responded = False

        if len(core.war_ticker_instance.wars) > 0:
            max_id_length = 1
            max_duration_length = 1

            for war in core.war_ticker_instance.wars:
                war_id = "{:d}-{:d}".format(war.year, war.war_id)
                if len(war_id) > max_id_length:
                    max_id_length = len(war_id)

                war_duration = war.get_duration_text(war.duration())
                if len(war_duration) > max_duration_length:
                    max_duration_length = len(war_duration)

            for war in core.war_ticker_instance.wars:
                if all_wars or war.channel.lower() == command_data.channel.lower():
                    if all:
                        output = war.get_description_with_channel(max_id_length, max_duration_length)
                    else:
                        output = war.get_description(max_id_length, max_duration_length)

                    self.respond_to_user(connection, event, output)
                    responded = True

        if not responded:
            self.respond_to_user(connection, event, "No wars are current available.")

    def _find_join_leave_war(self, connection, event, command_data: CommandData):
        if event.type == "privmsg":
            self.respond_to_user(connection, event, "Sorry. This currently only works in regular channels.")
            return None
        else:
            channel_data = bot_instance.channels[command_data.channel]

        war = None

        if command_data.arg_count == 1:
            if channel_data.newest_war_id != "":
                war = self._find_war_by_id(channel_data.newest_war_id)
            else:
                self.respond_to_user(connection, event,
                                     "Hmmm. I don't seem to have a record of an upcoming war. Try providing a war ID?")
                self.respond_to_user(connection, event, "Usage: !war {} [<war id>]".format(command_data.args[0]))
        else:
            war = self._find_war_by_id(command_data.args[1])

        if war is None:
            self.respond_to_user(connection, event, "That war id was not found.")

        return war

    @staticmethod
    def _find_war_by_id(search_id):
        for war in core.war_ticker_instance.wars:
            war_id = "{:d}-{:d}".format(war.year, war.war_id)
            if war_id == search_id:
                return war

        return None

    @staticmethod
    def _find_war_by_name(search_name, channel):
        return_war = None

        for war in core.war_ticker_instance.wars:
            if war.get_internal_name().lower() == search_name.lower() and war.channel.lower() == channel.lower():
                if return_war is None:
                    return_war = war
                else:
                    return None

        return return_war

    def _remove_war(self, connection, event, command_data: CommandData, war: WordWar):
        if command_data.issuer.lower() == war.starter.lower():
            core.war_ticker_instance.wars.remove(war)
            war.cancel_war()
            self.respond_to_user(connection, event, "{} has been ended.".format(war.get_name()))
        else:
            self.respond_to_user(connection, event, "Only the starter of a war can end it early.")
