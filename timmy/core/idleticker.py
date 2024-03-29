import random
from datetime import datetime

import schedule
from irc.dict import IRCDict

from timmy import core, utilities
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData
from timmy.data.command_type import CommandType
from timmy.db_access import chainstory_db
from timmy.utilities import markov_generator


class IdleTicker:
    def __init__(self):
        self.amusement_command_processors = IRCDict()

    def init(self) -> None:
        schedule.every(1).minutes.do(deidle_timer_loop).tag('idleticker')

    def reset_timer(self) -> None:
        schedule.clear('idleticker')
        self.init()

    def tick(self) -> None:
        today = datetime.now()

        self.novel_writing_tick()

        # TODO: Add twitter deidle code here

        channel: ChannelData
        for channel in core.bot_instance.channels.values():
            if today.minute % 30 == 0 and channel.raptor_data['strength'] > 0:
                channel.raptor_data['strength'] -= round(channel.raptor_data['strength'] * 0.01)
                channel.save_data()

            channel.clear_timed_muzzle()

            if channel.is_muzzled():
                continue

            if random.random() * 100 < channel.chatter_settings['random_level']:
                actions = []

                if channel.chatter_settings['types']['markov']:
                    actions.append('markov')
                    actions.append('markov')
                    actions.append('markov')
                    actions.append('markov')

                if channel.amusement_chatter_available():
                    actions.append('amusement')
                    actions.append('amusement')

                if channel.chatter_settings['types']['bored']:
                    actions.append('bored')

                if channel.chatter_settings['types']['groot']:
                    actions.append('groot')

                if channel.chatter_settings['types']['velociraptor']:
                    actions.append('velociraptor')

                if len(actions) == 0:
                    continue

                action = random.choice(actions)

                if action == 'markov':
                    markov_generator.random_action(channel, 'say' if random.getrandbits(1) else 'emote', '')
                elif action == 'amusement':
                    self.amusement_tick(channel)
                elif action == 'velociraptor':
                    core.raptor_ticker.swarm(channel)
                elif action == 'bored':
                    channel.send_message("I'm bored.")
                elif action == 'groot':
                    channel.send_message(utilities.text_generator.get_string("[groot]")[:480])

    def amusement_tick(self, channel: ChannelData) -> None:
        amusement_commands = list(self.amusement_command_processors.keys())
        selected_command = random.choice(amusement_commands)

        amusement_command = CommandData()
        amusement_command.type = CommandType.TIMMY_USER
        amusement_command.issuer = "Timmy"
        amusement_command.channel = channel.name
        amusement_command.command = selected_command
        amusement_command.args = []
        amusement_command.arg_count = 0
        amusement_command.arg_string = ""
        amusement_command.prefix = "!"
        amusement_command.automatic = True
        amusement_command.in_pm = False

        self.amusement_command_processors[selected_command].process_amusement(amusement_command)

    def novel_writing_tick(self):
        today = datetime.now()
        is_november = today.month == 11

        if is_november:
            wordcount = chainstory_db.word_count()
            expected_wordcount = today.day * 50000 / 30.0

            relative_pace = max(0.5, float(wordcount) / expected_wordcount)
            odds_of_writing = 2.5 / (relative_pace * relative_pace)

            if random.random() * 100 < odds_of_writing:
                last_lines = chainstory_db.get_last_lines()
                new_line = markov_generator.generate_markov('novel', last_lines[len(last_lines) - 1])

                chainstory_db.add_line(new_line, "Timmy")

                channel: ChannelData
                for channel in core.bot_instance.channels.values():
                    if random.randint(0, 100) > 15:
                        continue

                    if channel.chatter_settings['types']['chainstory'] and not channel.is_muzzled() and \
                            channel.chatter_settings['random_level'] > 0:
                        core.bot_instance.connection.action(channel.name, "opens up his novel file, considers for a "
                                                                          "minute, and then rapidly types in several "
                                                                          "words. (Help Timmy out by using the Chain "
                                                                          "Story commands. See !help for information.)")


def deidle_timer_loop() -> None:
    try:
        core.idle_ticker.tick()
    except Exception:
        from timmy.utilities import irc_logger
        irc_logger.log_traceback()

