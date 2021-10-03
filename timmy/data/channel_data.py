import copy
import time
from typing import Optional

from irc.bot import Channel
from irc.dict import IRCDict

from timmy import core, db_access
from timmy.data.war_state import WarState


class ChannelData(Channel):
    current_odds: IRCDict
    max_odds = IRCDict(
            {
                'answer':       65,
                'aypwip':       100,
                'cheeseburger': 50,
                'eightball':    100,
                'fox':          75,
                'groot':        100,
                'hug':          100,
                'lights':       100,
                'soon':         100,
                'test':         100,
                'tissue':       100,
            }
    )

    chatter_settings: IRCDict
    chatter_settings_defaults = IRCDict(
            {
                'reactive_level':  2.5,
                'random_level':    1,
                'name_multiplier': 1.5,
                'types':           IRCDict(
                        {
                            'banish':            True,
                            'bored':             False,
                            'catch':             True,
                            'chainstory':        True,
                            'challenge':         True,
                            'dance':             True,
                            'defenestrate':      True,
                            'eightball':         True,
                            'foof':              True,
                            'fridge':            True,
                            'get':               True,
                            'greetings':         True,
                            'groot':             True,
                            'helpful_reactions': True,
                            'herd':              True,
                            'markov':            True,
                            'silly_reactions':   True,
                            'sing':              True,
                            'summon':            True,
                            'velociraptor':      True,
                        }
                )
            }
    )

    raptor_data: IRCDict
    raptor_data_defaults = IRCDict(
            {
                'sightings': 0,
                'active':    0,
                'dead':      0,
                'killed':    0,
                'strength':  0
            }
    )

    twitter_settings: IRCDict
    twitter_settings_defaults = IRCDict(
            {
                'time':               0,
                'bucket':             5.0,
                'bucket_max':         10.0,
                'bucket_charge_rate': 0.05,
            }
    )

    command_settings: IRCDict
    command_defaults = IRCDict(
            {
                'attack':       True,
                'banish':       True,
                'catch':        True,
                'chainstory':   True,
                'challenge':    True,
                'commandment':  True,
                'defenestrate': True,
                'dance':        True,
                'dice':         True,
                'eightball':    True,
                'expound':      True,
                'foof':         True,
                'fridge':       True,
                'get':          True,
                'herd':         True,
                'lick':         False,
                'pickone':      True,
                'ping':         True,
                'search':       True,
                'sing':         True,
                'summon':       True,
                'velociraptor': True,
                'woot':         True
            }
    )

    def __init__(self, name):
        super().__init__()

        self.name = name

        self.last_speaker = ""
        self.last_speaker_time = 0.0

        self.last_war_id = ""
        self.newest_war_id = ""

        self._muzzled = False
        self.muzzled_until = None
        self.auto_muzzle = True

        self.command_settings = copy.deepcopy(ChannelData.command_defaults)
        self.chatter_settings = copy.deepcopy(ChannelData.chatter_settings_defaults)
        self.current_odds = copy.deepcopy(ChannelData.max_odds)

        self.twitter_accounts = []
        self.twitter_settings = copy.deepcopy(ChannelData.twitter_settings_defaults)

        self.raptor_data = copy.deepcopy(ChannelData.raptor_data_defaults)

    def join_channel(self) -> None:
        db_access.channel_db.join_channel(self)

    def leave_channel(self) -> None:
        db_access.channel_db.deactivate_channel(self)

    def save_data(self) -> None:
        db_access.channel_db.save_channel_settings(self)

    def load_data(self) -> None:
        db_access.channel_db.load_channel_data(self)

    def set_defaults(self) -> None:
        self.current_odds = copy.deepcopy(ChannelData.max_odds)
        self.twitter_settings = copy.deepcopy(ChannelData.twitter_settings_defaults)
        self.chatter_settings = copy.deepcopy(ChannelData.chatter_settings_defaults)
        self.command_settings = copy.deepcopy(ChannelData.command_defaults)
        self.raptor_data = copy.deepcopy(ChannelData.raptor_data_defaults)

    def amusement_chatter_available(self) -> bool:
        amusements = ['get', 'eightball', 'fridge', 'defenestrate', 'sing', 'foof', 'dance', 'summon', 'catch',
                      'search', 'herd', 'banish']

        for amusement in amusements:
            if self.chatter_settings['types'][amusement]:
                return True

        return False

    def is_muzzled(self) -> bool:
        auto_muzzle = False

        for war in core.war_ticker.active_wars:
            if self.auto_muzzle and war.state is WarState.ACTIVE and self.name.lower() == war.channel.lower():
                auto_muzzle = True
                break

        return self._muzzled or auto_muzzle

    def clear_timed_muzzle(self) -> None:
        if self._muzzled and self.muzzled_until is not None and (self.muzzled_until < time.time()):
            self._muzzled = False
            self.muzzled_until = 0.0
            self.save_data()

    def set_muzzle_flag(self, muzzled: bool, timeout: Optional[float] = None) -> None:
        self._muzzled = muzzled
        self.muzzled_until = timeout
        self.save_data()

    def set_auto_muzzle(self, flag: bool) -> None:
        self.auto_muzzle = flag
        self.save_data()

    def set_chatter_flags(self, reactive: float, name: float, random: float) -> None:
        self.chatter_settings['reactive_level'] = reactive
        self.chatter_settings['name_multiplier'] = name
        self.chatter_settings['random_level'] = random
        self.save_data()

    def record_sighting(self, new_raptors: int = 1) -> None:
        self.raptor_data['sightings'] += new_raptors
        self.raptor_data['active'] += new_raptors
        self.save_data()

    def record_new_raptors(self, new_raptors: int) -> None:
        self.raptor_data['active'] += new_raptors
        self.save_data()

    def record_leaving_raptors(self, leaving_raptors: int) -> None:
        self.raptor_data['active'] -= leaving_raptors

        if self.raptor_data['active'] < 0:
            self.raptor_data['active'] = 0

        self.save_data()

    def record_kills(self, kills: int) -> None:
        self.raptor_data['killed'] += kills
        self.save_data()

    def record_deaths(self, deaths: int) -> None:
        self.raptor_data['active'] -= deaths
        self.raptor_data['dead'] += deaths

        if self.raptor_data['active'] < 0:
            self.raptor_data['active'] = 0

        self.save_data()

    def send_message(self, message: str) -> None:
        core.bot_instance.connection.privmsg(self.name, message)

    def send_action(self, message: str) -> None:
        core.bot_instance.connection.action(self.name, message)
