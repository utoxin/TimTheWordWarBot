import random
from datetime import datetime, timedelta

from irc.client import Event
from irc.dict import IRCDict
from timeloop import Timeloop

from timmy import core
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData
from timmy.data.command_type import CommandType
from timmy.db_access import chainstory_db
from timmy.utilities import markov_generator

idle_timer = Timeloop()


class IdleTicker:
    def __init__(self):
        self.amusement_command_processors = IRCDict()

    def init(self) -> None:
        idle_timer.start()

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

                if channel.amusement_chatter_available():
                    actions.append('amusement')

                if channel.chatter_settings['types']['bored']:
                    actions.append('bored')

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

    def amusement_tick(self, channel: ChannelData) -> None:
        amusement_commands = list(self.amusement_command_processors.keys())
        selected_command = amusement_commands[random.randint(0, len(amusement_commands))]

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

        event = Event('privmsg', 'Timmy', channel.name)

        self.amusement_command_processors[selected_command].process_amusement(event, amusement_command)

    def novel_writing_tick(self):
        today = datetime.now()
        is_november = today.month == 11

        # TESTING
        is_november = True

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
                        core.bot_instance.connection.action(channel, "opens up his novel file, considers for a minute, "
                                                                     "and then rapidly types in several words. (Help "
                                                                     "Timmy out by using the Chain Story commands. "
                                                                     "See !help for information.)")


@idle_timer.job(interval = timedelta(seconds = 60))
def deidle_timer_loop() -> None:
    core.idle_ticker.tick()
