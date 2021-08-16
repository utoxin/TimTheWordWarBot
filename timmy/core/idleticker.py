import random
from datetime import timedelta, datetime

from timeloop import Timeloop

from timmy import core
from timmy.data.channel_data import ChannelData
from timmy.utilities import markov_generator, irc_logger

idle_timer = Timeloop()


class IdleTicker:
    @staticmethod
    def init() -> None:
        idle_timer.start()

    @staticmethod
    def tick() -> None:
        today = datetime.now()
        is_november = today.month == 11

        # TODO: Add novel writing ticker here

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
                    # TODO: Handle amusement stuff here
                    continue
                elif action == 'velociraptor':
                    core.raptor_ticker.swarm(channel)
                elif action == 'bored':
                    channel.send_message("I'm bored.")


@idle_timer.job(interval=timedelta(seconds=60))
def deidle_timer_loop() -> None:
    core.idle_ticker.tick()
