import random
from datetime import datetime
from typing import Optional

from irc.client import Event, ServerConnection

from timmy import core
from timmy.data.channel_data import ChannelData
from timmy.data.userdata import UserData
from timmy.data.word_war import WordWar
from timmy.utilities import text_generator


class RaptorTicker:
    @staticmethod
    def _channel_raptor_cap(channel_data: ChannelData) -> int:
        return 100 + channel_data.raptor_data['strength']

    def sighting(self, connection: ServerConnection, event: Event) -> None:
        action = event.type == 'action'
        channel_data: ChannelData = core.bot_instance.channels[event.target]

        if channel_data is None:
            return

        raptor_cap = self._channel_raptor_cap(channel_data)

        if random.randrange(raptor_cap) < max(10, raptor_cap - channel_data.raptor_data['active']):
            channel_data.record_sighting()
            if action:
                core.bot_instance.connection.action(
                        channel_data.name,
                        text_generator.get_string("[raptor_sighting_action_response]")
                )
            else:
                core.bot_instance.connection.privmsg(
                        channel_data.name,
                        text_generator.get_string("[raptor_sighting_message_response]")
                )
        elif random.randrange(100) < 25:
            if action:
                core.bot_instance.connection.action(
                        channel_data.name,
                        text_generator.get_string("[raptor_sighting_old_action_response]")
                )
            else:
                core.bot_instance.connection.privmsg(
                        channel_data.name,
                        text_generator.get_string("[raptor_sighting_old_message_response]")
                )

    def swarm(self, channel_data: ChannelData) -> None:
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

    def war_report(self, user_data: UserData, war: WordWar, wordcount: int) -> None:
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

    def _attack_channel(self, source_channel: ChannelData) -> None:
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

                core.bot_instance.connection.privmsg(
                        source_channel.name,
                        self._attack_message(
                                defending_channel,
                                attack_count,
                                defender_deaths,
                                attack_count - attacker_deaths
                        )
                )

                core.bot_instance.connection.privmsg(
                        defending_channel.name,
                        self._defense_message(
                                source_channel,
                                attack_count,
                                defender_deaths
                        )
                )

    def _colonize_channel(self, source_channel: ChannelData) -> None:
        max_colony = source_channel.raptor_data['active'] // 4

        if max_colony <= 0:
            return

        colony_count = random.randint(1, max_colony)
        colonize_channel: ChannelData = self._select_low_population_raptor_channel(source_channel)

        if colonize_channel is not None:
            colonize_channel.record_new_raptors(colony_count)
            source_channel.record_leaving_raptors(colony_count)

            core.bot_instance.connection.privmsg(
                    source_channel.name, f"Apparently feeling crowded, {colony_count} of the velociraptors head off in "
                                         f"search of new territory. After searching, they settle in "
                                         f"{colonize_channel.name}."
            )

            core.bot_instance.connection.privmsg(
                    colonize_channel.name, f"A swarm of {colony_count} velociraptors appears from the direction of "
                                           f"{source_channel.name}. The local raptors are nervous, but the strangers "
                                           f"simply want to join the colony."
            )

    def _hatch_raptors(self, source_channel: ChannelData) -> None:
        if source_channel.raptor_data['active'] > 1:
            new_count = random.randrange(source_channel.raptor_data['active'] // 2)

            if source_channel.raptor_data['active'] >= 4:
                new_count -= random.randrange(source_channel.raptor_data['active'] // 4)

            if new_count < 1:
                return

            source_channel.record_sighting(new_count)

            core.bot_instance.connection.privmsg(source_channel.name, self._hatching_message(new_count))

    @staticmethod
    def _select_high_population_raptor_channel(exclude_channel: ChannelData) -> Optional[ChannelData]:
        candidates = [v for (k, v) in core.bot_instance.channels if v.chatter_settings['types']['raptor'] and
                      v.raptor_data['active'] > 0 and not v.is_muzzled()]
        candidates.sort(key = lambda c: c.raptor_data['active'], reverse = True)

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
    def _select_low_population_raptor_channel(exclude_channel: ChannelData) -> Optional[ChannelData]:
        candidates = [v for (k, v) in core.bot_instance.channels if v.chatter_settings['types']['raptor'] and
                      v.raptor_data['active'] > 0 and not v.is_muzzled()]
        candidates.sort(key = lambda c: c.raptor_data['active'])

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
    def _select_random_raptor_channel(exclude_channel: ChannelData) -> Optional[ChannelData]:
        candidates = [v for (k, v) in core.bot_instance.channels if v.chatter_settings['types']['raptor'] and
                      v.raptor_data['active'] > 0 and not v.is_muzzled()]

        candidates.remove(exclude_channel)

        if len(candidates) == 0:
            return None
        else:
            random.shuffle(candidates)
            return candidates[0]

    def _brag_about_word_count(self, user_data: UserData, war_data: WordWar, word_count: int) -> None:
        war_channel = core.bot_instance.channels[war_data.channel]
        selected_channel = self._select_random_raptor_channel(war_channel)

        if selected_channel is None:
            return

        origin_message = text_generator.get_string(
                "[raptor_brag_origin]", {
                    'raptor_name':  user_data.raptor_name,
                    'channel_name': selected_channel.name,
                }
        )

        destination_message = text_generator.get_string(
                "[raptor_brag_destination]", {
                    'raptor_name':  user_data.raptor_name,
                    'channel_name': war_channel.name,
                    'word_count':   "{:n}".format(word_count),
                }
        )

        core.bot_instance.connection.privmsg(war_channel.name, origin_message)
        core.bot_instance.connection.privmsg(selected_channel.name, destination_message)

    def _steal_plot_bunnies(self, user_data: UserData, war_data: WordWar) -> None:
        war_channel = core.bot_instance.channels[war_data.channel]
        selected_channel = self._select_random_raptor_channel(war_channel)

        if selected_channel is None:
            return

        bunny_count = random.randrange(10)
        threshold = 0

        if bunny_count > 1:
            threshold = 2
        elif bunny_count > 0:
            threshold = 1

        origin_message = text_generator.get_string(
                "[raptor_steal_bunnies_origin_{:d}]".format(threshold), {
                    'raptor_name':  user_data.raptor_name,
                    'channel_name': selected_channel.name,
                    'bunny_count':  "{:n}".format(bunny_count),
                }
        )

        destination_message = text_generator.get_string(
                "[raptor_steal_bunnies_destination_{:d}".format(threshold), {
                    'raptor_name':  user_data.raptor_name,
                    'channel_name': war_channel.name,
                    'bunny_count':  "{:n}".format(bunny_count),
                }
        )

        core.bot_instance.connection.privmsg(war_channel.name, origin_message)
        core.bot_instance.connection.privmsg(selected_channel.name, destination_message)

        user_data.raptor_bunnies_stolen += bunny_count
        user_data.last_bunny_raid = datetime.now()
        user_data.save()

    def _recruit_raptors(self, user_data: UserData, war_data: WordWar) -> None:
        war_channel = core.bot_instance.channels[war_data.channel]
        selected_channel = self._select_high_population_raptor_channel(war_channel)

        if selected_channel is None:
            return

        raptor_count = random.randrange(selected_channel.raptor_data['active'] // 10)
        threshold = 0

        if raptor_count > 1:
            threshold = 2
        elif raptor_count > 0:
            threshold = 1

        origin_message = text_generator.get_string(
                "[raptor_recruit_origin_{:d}]".format(threshold), {
                    'raptor_name':  user_data.raptor_name,
                    'channel_name': selected_channel.name,
                    'raptor_count': "{:n}".format(raptor_count),
                }
        )

        destination_message = text_generator.get_string(
                "[raptor_recruit_destination_{:d}".format(threshold), {
                    'raptor_name':  user_data.raptor_name,
                    'channel_name': war_channel.name,
                    'raptor_count': "{:n}".format(raptor_count),
                }
        )

        core.bot_instance.connection.privmsg(war_channel.name, origin_message)
        core.bot_instance.connection.privmsg(selected_channel.name, destination_message)

        war_channel.record_sighting(raptor_count)
        selected_channel.raptor_data['active'] -= raptor_count
        if selected_channel.raptor_data['active'] < 0:
            selected_channel.raptor_data['active'] = 0
        selected_channel.save_data()

    @staticmethod
    def _attack_message(channel: ChannelData, attack_count: int, defender_deaths: int, attack_survivors: int) -> str:
        return text_generator.get_string(
                "[raptor_attack_message]", {
                    'attack_count': "{:n}".format(attack_count),
                    'channel_name': channel.name,
                    'kill_count':   "{:n}".format(defender_deaths),
                    'return_count': "{:n}".format(attack_survivors)
                }
        )

    @staticmethod
    def _defense_message(channel: ChannelData, attack_count: int, defender_deaths: int) -> str:
        return text_generator.get_string(
                "[raptor_defense_message]", {
                    'attack_count': "{:n}".format(attack_count),
                    'channel_name': channel.name,
                    'kill_count':   "{:n}".format(defender_deaths)
                }
        )

    @staticmethod
    def _hatching_message(new_count: int) -> str:
        if new_count == 1:
            return text_generator.get_string("[raptor_hatch_message_1]")
        else:
            return text_generator.get_string(
                    "[raptor_hatch_message_2]", {
                        'raptor_count': "{:n}".format(new_count)
                    }
            )
