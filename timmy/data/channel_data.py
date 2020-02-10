class ChannelData:
    max_odds = {
        'answer':        65,
        'aypwip':       100,
        'cheeseburger':  50,
        'eightball':    100,
        'fox':           75,
        'groot':        100,
        'hug':          100,
        'lights':       100,
        'soon':         100,
        'test':         100,
        'tissue':       100,
    }

    chatter_settings_defaults = {
        'reactive_level': 2.5,
        'random_level': 1,
        'name_multiplier': 1.5
    }

    raptor_data_defaults = {
        'sightings': 0,
        'active': 0,
        'dead': 0,
        'killed': 0,
        'strength': 0
    }

    twitter_settings_defaults = {
        'time': 0,
        'bucket': 5.0,
        'bucket_max': 10.0,
        'bucket_charge_rate': 0.05,
    }

    chatter_defaults = {
        'banish': True,
        'bored': False,
        'catch': True,
        'chainstory': True,
        'challenge': True,
        'dance': True,
        'defenestrate': True,
        'eightball': True,
        'foof': True,
        'fridge': True,
        'get': True,
        'greetings': True,
        'groot': True,
        'helpful_reactions': True,
        'herd': True,
        'markov': True,
        'silly_reactions': True,
        'sing': True,
        'summon': True,
        'velociraptor': True,
    }

    command_defaults = {
        'attack': True,
        'banish': True,
        'catch': True,
        'chainstory': True,
        'challenge': True,
        'commandment': True,
        'defenestrate': True,
        'dance': True,
        'dice': True,
        'eightball': True,
        'expound': True,
        'foof': True,
        'fridge': True,
        'get': True,
        'herd': True,
        'lick': False,
        'ping': True,
        'search': True,
        'sing': True,
        'summon': True,
        'velociraptor': True,
        'woot': True
    }

    def __init__(self, channel):
        self.channel = channel

        self.last_speaker = ""
        self.last_speaker_time = 0

        self.last_war_id = ""
        self.newest_war_id = ""

        self.muzzled = False
        self.muzzled_until = 0
        self.auto_muzzle = True

        self.chatter_enabled = ChannelData.chatter_defaults
        self.commands_enabled = ChannelData.command_defaults
        self.chatter_settings = ChannelData.chatter_settings_defaults
        self.current_odds = ChannelData.max_odds

        self.twitter_accounts = {}
        self.twitter_settings = ChannelData.twitter_settings_defaults

        self.raptor_data = ChannelData.raptor_data_defaults

    def set_defaults(self):
        self.chatter_settings = ChannelData.chatter_settings_defaults
        self.current_odds = ChannelData.max_odds
        self.twitter_settings = ChannelData.twitter_settings_defaults
        self.chatter_enabled = ChannelData.chatter_defaults
        self.commands_enabled = ChannelData.command_defaults
        self.raptor_data = ChannelData.raptor_data_defaults

    def amusement_chatter_available(self):
        amusements = {'get', 'eightball', 'fridge', 'defenestrate', 'sing', 'foof', 'dance', 'summon', 'catch',
                      'search', 'herd', 'banish'}

        return any([self.chatter_settings[x] for x in amusements])
