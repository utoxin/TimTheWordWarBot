import time

from irc.bot import Channel
from irc.dict import IRCDict

from timmy import db_access


class ChannelData(Channel):
    max_odds = IRCDict({
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
    })

    chatter_settings_defaults = IRCDict({
        'reactive_level':  2.5,
        'random_level':    1,
        'name_multiplier': 1.5,
        'types':           IRCDict({
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
        })
    })

    raptor_data_defaults = IRCDict({
        'sightings': 0,
        'active':    0,
        'dead':      0,
        'killed':    0,
        'strength':  0
    })

    twitter_settings_defaults = IRCDict({
        'time':               0,
        'bucket':             5.0,
        'bucket_max':         10.0,
        'bucket_charge_rate': 0.05,
    })

    command_defaults = IRCDict({
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
        'ping':         True,
        'search':       True,
        'sing':         True,
        'summon':       True,
        'velociraptor': True,
        'woot':         True
    })

    def __init__(self, name):
        super().__init__()

        self.name = name

        self.last_speaker = ""
        self.last_speaker_time = 0.0

        self.last_war_id = ""
        self.newest_war_id = ""

        self.muzzled = False
        self.muzzled_until = None
        self.auto_muzzle = True

        self.command_settings = ChannelData.command_defaults
        self.chatter_settings = ChannelData.chatter_settings_defaults
        self.current_odds = ChannelData.max_odds

        self.twitter_accounts = {}
        self.twitter_settings = ChannelData.twitter_settings_defaults

        self.raptor_data = ChannelData.raptor_data_defaults

    def join_channel(self):
        db_access.channel_db.join_channel(self)

    def leave_channel(self):
        db_access.channel_db.deactivate_channel(self)

    def save_data(self):
        db_access.channel_db.save_channel_settings(self)

    def load_data(self):
        db_access.channel_db.load_channel_data(self)

    def set_defaults(self):
        self.current_odds = ChannelData.max_odds
        self.twitter_settings = ChannelData.twitter_settings_defaults
        self.chatter_settings = ChannelData.chatter_settings_defaults
        self.command_settings = ChannelData.command_defaults
        self.raptor_data = ChannelData.raptor_data_defaults

    def amusement_chatter_available(self):
        amusements = {'get', 'eightball', 'fridge', 'defenestrate', 'sing', 'foof', 'dance', 'summon', 'catch',
                      'search', 'herd', 'banish'}

        return any([self.chatter_settings['types'][x] for x in amusements])

    def is_muzzled(self):
        # TODO: Update this after wars are implemented
        return self.muzzled

    def clear_timed_muzzle(self):
        if self.muzzled and self.muzzled_until is not None and (self.muzzled_until < time.time()):
            self.muzzled = False
            self.muzzled_until = 0.0
