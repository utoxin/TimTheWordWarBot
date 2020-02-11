import uuid
from datetime import datetime

from timmy.data.war_state import WarState


class WordWar:
    def __init__(self):
        self.year = 0
        self.war_id = 0
        self.uuid = None
        self.channel = ""
        self.starter = ""
        self.name = ""
        self.base_duration = 0
        self.base_break = 0
        self.total_chains = 0
        self.current_chain = 0
        self.start_epoch = 0.0
        self.end_epoch = 0.0
        self.randomness = False
        self.state = None
        self.channel_data = None
        self.war_members = None

    def basic_setup(self, channel_data, starter, name, base_duration, start_epoch):
        self.uuid = uuid.uuid4()
        self.channel = channel_data.channel
        self.starter = starter
        self.name = name
        self.base_duration = base_duration
        self.start_epoch = start_epoch
        self.end_epoch = start_epoch + base_duration
        self.total_chains = 1
        self.current_chain = 1
        self.randomness = False
        self.state = WarState.PENDING
        self.channel_data = channel_data
        self.year = datetime.today().year

    def chain_setup(self, channel_data, starter, name, base_duration, start_epoch, total_chains, base_break,
                    randomness):
        self.uuid = uuid.uuid4()
        self.channel = channel_data.channel
        self.starter = starter
        self.name = name
        self.base_duration = base_duration
        self.base_break = base_break
        self.start_epoch = start_epoch
        self.end_epoch = start_epoch + base_duration
        self.total_chains = total_chains
        self.current_chain = 1
        self.randomness = randomness
        self.state = WarState.PENDING
        self.channel_data = channel_data
        self.year = datetime.today().year

    def load_from_db(self, year, war_id, uuid, channel, starter, name, base_duration, base_break, total_chains,
                     current_chain, start_epoch, end_epoch, randomness, state):
        self.uuid = uuid