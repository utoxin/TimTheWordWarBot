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

    def war_report(self, user_data: UserData, war: WordWar, wordcount: int):
        actions = [
            'brag',
            'bunnies',
            'recruit'
        ]

        wpm = wordcount / (war.base_duration / 60)

        random.shuffle(actions)

        for action in actions:
            if random.randrange(100) < (wpm / 10):
                if action == "brag":
                    self._brag_about_word_count(user_data, war, wordcount)
                elif action == "bunnies":
                    self._steal_plot_bunnies(user_data, war)
                elif action == "recruit":
                    self._recruit_raptors(user_data, war)

                break

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

                bot_instance.connection.privmsg(source_channel.name, self._attack_message(defending_channel.name,
                                                                                          attack_count,
                                                                                          defender_deaths,
                                                                                          attack_count - attacker_deaths
                                                                                          ))

                bot_instance.connection.privmsg(defending_channel.name, self._defense_message(source_channel.name,
                                                                                              attack_count,
                                                                                              defender_deaths))

    def _colonize_channel(self, source_channel: ChannelData):
        max_colony = source_channel.raptor_data['active'] // 4

        if max_colony <= 0:
            return

        colony_count = random.randint(1, max_colony)
        colonize_channel: ChannelData = self._select_low_population_raptor_channel(source_channel)

        if colonize_channel is not None:
            colonize_channel.record_new_raptors(colony_count)
            source_channel.record_leaving_raptors(colony_count)

            bot_instance.connection.privmsg(source_channel.name, "Apparently feeling crowded, {} of the velociraptors "
                                                                 "head off in search of new territory. After "
                                                                 "searching, they settle in {}.".format(colony_count,
                                                                                                        colonize_channel
                                                                                                        .name))

            bot_instance.connection.privmsg(colonize_channel.name, "A swarm of {} velociraptors appears from the "
                                                                   "direction of {}. The local raptors are nervous, "
                                                                   "but the strangers simply want to join the "
                                                                   "colony.".format(colony_count,
                                                                                    source_channel.name))

    def _hatch_raptors(self, source_channel):
        if source_channel.raptor_data['active'] > 1:
            new_count = random.randrange(source_channel.raptor_data['active'] // 2)

            if source_channel.raptor_data['active'] >= 4:
                new_count -= random.randrange(source_channel.raptor_data['active'] // 4)

            if new_count < 1:
                return

            source_channel.record_sighting(new_count)

            bot_instance.connection.privmsg(source_channel.name, self._hatching_message(new_count))

    @staticmethod
    def _select_high_population_raptor_channel(exclude_channel):
        candidates = [c for c in bot_instance.channels if c.chatter_settings['types']['raptor'] and
                      c.raptor_data['active'] > 0 and not c.is_muzzled()]
        candidates.sort(key=lambda c: c.raptor_data['active'], reverse=True)

        candidates.remove(exclude_channel)

        total_raptors = sum(c.raptor_data['active'] for c in candidates)

        if total_raptors == 0:
            return None
        else:
            magic_number = random.randrange(total_raptors)
            current_number = 0

            for channel in candidates:
                current_number += channel.raptor_data['active']
                if current_number <= magic_number:
                    return channel

            return candidates[0]

    @staticmethod
    def _select_low_population_raptor_channel(exclude_channel):
        candidates = [c for c in bot_instance.channels if c.chatter_settings['types']['raptor'] and
                      c.raptor_data['active'] > 0 and not c.is_muzzled()]
        candidates.sort(key=lambda c: c.raptor_data['active'])

        candidates.remove(exclude_channel)

        total_raptors = sum(c.raptor_data['active'] for c in candidates)
        max_raptors = max(c.raptor_data['active'] for c in candidates)

        if total_raptors == 0:
            return None
        else:
            magic_number = random.randrange(total_raptors)
            current_number = 0

            for channel in candidates:
                current_number += max_raptors - channel.raptor_data['active']
                if current_number <= magic_number:
                    return channel

            return candidates[0]

    @staticmethod
    def _select_random_raptor_channel(exclude_channel):
        candidates = [c for c in bot_instance.channels if c.chatter_settings['types']['raptor'] and
                      c.raptor_data['active'] > 0 and not c.is_muzzled()]

        candidates.remove(exclude_channel)

        if len(candidates) == 0:
            return None
        else:
            random.shuffle(candidates)
            return candidates[0]

    def _brag_about_word_count(self, user_data: UserData, war_data: WordWar, word_count):
        pass

    def _steal_plot_bunnies(self, user_data: UserData, war_data: WordWar):
        pass

    def _recruit_raptors(self, user_data: UserData, war_data: WordWar):
        pass

    def _attack_message(self, channel, attack_count, defender_deaths, attack_survivors):
        pass

    def _defense_message(self, channel, attack_count, defender_deaths):
        pass

    def _hatching_message(self, new_count):
        pass
