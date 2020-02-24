import random
import time
from datetime import timedelta

from timeloop import Timeloop

from timmy import core
from timmy.data.war_state import WarState
from timmy.data.word_war import WordWar
from timmy.db_access import word_war_db

war_timer = Timeloop()


class WarTicker:
    def __init__(self):
        self.wars = set()

    def load_wars(self):
        self.wars = word_war_db.load_wars()
        war_timer.start()

    def begin_war(self, war: WordWar):
        current_epoch = time.time()
        late_start = current_epoch - war.start_epoch

        message = "%s: Starting now!".format(war.get_name())

        if late_start >= 5:
            message += " (%d seconds late. Sorry!)".format(late_start)

        core.bot_instance.connection.privmsg(war.channel, message)

        self.notify_war_members(war, message)

        war.begin_war()

    def end_war(self, war: WordWar):
        current_epoch = time.time()
        late_end = current_epoch - war.end_epoch

        message = "%s: Ending now!".format(war.get_name())

        if late_end >= 5:
            message += " (%d seconds late. Sorry!)".format(late_end)

        core.bot_instance.connection.privmsg(war.channel, message)

        self.notify_war_members(war, message)

        if war.channel in core.bot_instance.channels:
            core.bot_instance.channels[war.channel].last_war_id = war.get_id()

        if war.current_chain >= war.total_chains:
            war.end_war()
            self.wars.remove(war)

            if war.channel in core.bot_instance.channels \
                    and core.bot_instance.channels[war.channel].newest_war_id == war.get_id():
                core.bot_instance.channels[war.channel].newest_war_id = ""
        else:
            war.current_chain += 1

            if war.randomness:
                war.start_epoch = war.end_epoch + war.base_break + (war.base_break * (random.randrange(20) - 10)) / 100
                war.end_epoch = war.start_epoch + war.base_duration + (
                            war.base_duration * (random.randrange(20) - 10)) / 100
            else:
                war.start_epoch = war.end_epoch + war.base_break
                war.end_epoch = war.start_epoch + war.base_duration

            war.start_break()
            self.war_start_count(war)

            if war.channel in core.bot_instance.channels:
                core.bot_instance.channels[war.channel].newest_war_id = war.get_id()

    @staticmethod
    def war_start_count(war: WordWar):
        time_to_start = int(war.start_epoch - time.time())

        if time_to_start < 60:
            message = "%s: Starting in %d %s.".format(
                    [war.get_name(), time_to_start, "seconds" if time_to_start > 1 else "second"])
        else:
            minutes = time_to_start / 60
            if time_to_start % 60 == 0:
                message = "%s: Starting in %d %s.".format(
                        [war.get_name(include_duration=True), minutes, "minutes" if minutes > 1 else "minute"])
            else:
                message = "%s: Starting in %.1f minutes.".format([war.get_name(include_duration=True), minutes])

        core.bot_instance.connection.privmsg(war.channel, message)

    @staticmethod
    def war_end_count(war: WordWar):
        time_to_end = int(war.end_epoch - time.time())

        if time_to_end < 60:
            message = "%s: %d %s remaining!".format(
                    [war.get_name(), time_to_end, "seconds" if time_to_end > 1 else "second"])
        else:
            minutes = time_to_end / 60
            message = "%s: %d %s remaining.".format(
                    [war.get_name(), minutes, "minutes" if minutes > 1 else "minute"])

        core.bot_instance.connection.privmsg(war.channel, message)

    @staticmethod
    def notify_war_members(war: WordWar, message):
        for nick in war.war_members:
            core.bot_instance.connection.privmsg(nick, message)


@war_timer.job(interval=timedelta(seconds=1))
def war_update_loop():
    wars = core.war_ticker_instance.wars
    if wars is None or len(wars) <= 0:
        return

    current_epoch = time.time()

    for war in wars:
        if war.start_epoch >= current_epoch:
            time_difference = int(war.start_epoch - current_epoch)

            if time_difference in [600, 300, 60, 30, 5, 4, 3, 2, 1]:
                core.war_ticker_instance.war_start_count(war)
            elif time_difference == 0:
                core.war_ticker_instance.begin_war(war)
            elif time_difference >= 3600:
                if time_difference % 3600 == 0:
                    core.war_ticker_instance.war_start_count(war)
            elif time_difference >= 1800:
                if time_difference % 1800 == 0:
                    core.war_ticker_instance.war_start_count(war)
        else:
            if war.end_epoch >= current_epoch:
                if war.state == WarState.PENDING:
                    core.war_ticker_instance.begin_war(war)
                else:
                    time_difference = war.end_epoch - current_epoch

                    if time_difference in [600, 300, 60, 5, 4, 3, 2, 1]:
                        core.war_ticker_instance.war_end_count(war)
                    elif time_difference == 0:
                        core.war_ticker_instance.end_war(war)
                    elif time_difference >= 3600:
                        if time_difference % 3600 == 0:
                            core.war_ticker_instance.war_end_count(war)
                    elif time_difference >= 1800:
                        if time_difference % 1800 == 0:
                            core.war_ticker_instance.war_end_count(war)
            else:
                core.war_ticker_instance.end_war(war)
