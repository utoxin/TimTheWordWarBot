import random

from timmy.core import bot_instance
from timmy.data.channel_data import ChannelData
from timmy.data.userdata import UserData
from timmy.data.word_war import WordWar


class RaptorTicker:
    @staticmethod
    def _channel_raptor_cap(channel_data: ChannelData):
        return 100 + channel_data.raptor_data['strength']

    def sighting(self, connection, event):
        # TODO: Get action strings from the list code

        action = event.type == 'action'
        channel_data: ChannelData = bot_instance.channels[event.target]

        if channel_data is None:
            return

        raptor_cap = self._channel_raptor_cap(channel_data)

        if random.randrange(raptor_cap) < max(10, raptor_cap - channel_data.raptor_data['active']):
            channel_data.record_sighting()
            if action:
                # TODO: Send action response
                return
            else:
                # TODO: Send message response
                return
        elif random.randrange(100) < 25:
            if action:
                # TODO: Send action response
                return
            else:
                # TODO: Send message response
                return

    def swarm(self, channel_data: ChannelData):
        if random.randrange(100) <= 33:
            magic_number = random.randrange(100)
            raptor_cap = self._channel_raptor_cap(channel_data)

            threshold_number = 5 + round(37.5 * min(raptor_cap, channel_data.raptor_data['active']) / raptor_cap)

            if channel_data.raptor_data['active'] > raptor_cap:
                over_threshold = max(900, channel_data.raptor_data['active'] - raptor_cap)
                threshold_number += round(over_threshold / 100)

            if magic_number < threshold_number:
                self._attack_channel(channel_data)
            elif magic_number < (threshold_number * 2):
                self._colonize_channel(channel_data)
            else:
                self._hatch_raptors(channel_data)

    def _attack_channel(self, source_channel: ChannelData):
        max_attack = round(source_channel.raptor_data['active'] / 4)

        if max_attack <= 0:
            return

        attack_count = random.randrange(max_attack)

        if attack_count > 0:
            defending_channel: ChannelData = self._select_high_population_raptor_channel(source_channel)

            if defending_channel is not None:
                attacker_deaths = 0
                defender_deaths = 0

                attacker_bonus = source_channel.raptor_data['strength']
                defender_bonus = defending_channel.raptor_data['strength']

                fight_count = min(attack_count, defending_channel.raptor_data['active'])

                for i in range(0, fight_count):
                    attack_roll = round(random.random() * 100 * ((1 + attacker_bonus) / 100))
                    defense_roll = round(random.random() * 100 * ((1 + defender_bonus) / 100))

                    if attack_roll > defense_roll + 15:
                        defender_deaths += 1
                    elif defense_roll > attack_roll + 15:
                        attacker_deaths += 1
                    else:
                        if bool(random.getrandbits(1)):
                            defender_deaths += 1

                        if bool(random.getrandbits(1)):
                            attacker_deaths += 1

                source_channel.record_kills(defender_deaths)
                source_channel.record_deaths(attacker_deaths)

                defending_channel.record_kills(attacker_deaths)
                defending_channel.record_deaths(defender_deaths)

                # TODO: Send messages about the kills

    def _colonize_channel(self, source_channel):
        pass

    def _hatch_raptors(self, source_channel):
        pass

    def _select_high_population_raptor_channel(self, exclude_channel):
        pass

    def _select_low_population_raptor_channel(self, exclude_channel):
        pass

    def _select_random_raptor_channel(self, exclude_channel):
        pass

    def handle_war_report(self, user_data: UserData, war_data: WordWar, word_count):
        pass

    def _brag_about_word_count(self, user_data: UserData, war_data: WordWar, word_count):
        pass

    def _steal_plot_bunnies(self, user_data: UserData, war_data: WordWar):
        pass

    def _recruit_raptors(self, user_data: UserData, war_data: WordWar):
        pass
