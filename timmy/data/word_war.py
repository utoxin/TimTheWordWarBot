import time
import uuid
from datetime import datetime

import string

from timmy.data.war_state import WarState
from timmy.db_access import word_war_db


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

    def _prep_db_object(self):
        return {
            'year':          self.year,
            'uuid':          str(self.uuid),
            'channel':       self.channel,
            'starter':       self.starter,
            'name':          self.name,
            'base_duration': self.base_duration,
            'base_break':    self.base_break,
            'total_chains':  self.total_chains,
            'current_chain': self.current_chain,
            'start_epoch':   self.start_epoch,
            'end_epoch':     self.end_epoch,
            'randomness':    self.randomness,
            'war_state':     self.state
        }

    def basic_setup(self, channel_data, starter, name, base_duration, start_epoch):
        self.uuid = uuid.uuid4()
        self.channel = channel_data.name
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

        word_war_db.create_war(self._prep_db_object())

    def chain_setup(self, channel_data, starter, name, base_duration, start_epoch, total_chains, base_break,
                    randomness):
        self.uuid = uuid.uuid4()
        self.channel = channel_data.name
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

        word_war_db.create_war(self._prep_db_object())

    def load_from_db(self, year, war_id, uuid_string, channel, starter, name, base_duration, base_break, total_chains,
                     current_chain, start_epoch, end_epoch, randomness, state):
        self.uuid = uuid.UUID(uuid_string)
        self.year = year
        self.war_id = war_id
        self.channel = channel
        self.starter = starter
        self.name = name
        self.base_duration = base_duration
        self.base_break = base_break
        self.total_chains = total_chains
        self.current_chain = current_chain
        self.start_epoch = start_epoch
        self.end_epoch = end_epoch
        self.randomness = randomness
        self.state = state

    def end_war(self):
        self.state = WarState.FINISHED
        self.update_db()

    def cancel_war(self):
        self.state = WarState.CANCELLED
        self.update_db()

    def begin_war(self):
        self.state = WarState.ACTIVE
        self.update_db()

    def start_break(self):
        self.state = WarState.PENDING
        self.update_db()

    def update_db(self):
        word_war_db.update_war(self._prep_db_object())
        pass

    def duration(self):
        return self.end_epoch - self.start_epoch

    def get_name(self, include_id=False, include_duration=False, id_field_width=1, duration_field_width=1):
        formatter = string.Formatter()
        name_parts = []

        if include_id:
            db_id = self.get_id()
            name_parts.append(formatter.format("[ID %" + str(id_field_width) + "s]", db_id))

        if include_duration:
            name_parts.append(formatter.format("[%" + str(duration_field_width) + "s]",
                                               self.get_duration_text(self.duration())))

        name_parts.append(self.name)

        if self.total_chains > 1:
            name_parts.append(formatter.format("(%d/%d)", self.current_chain, self.total_chains))

        return " ".join(name_parts)

    @staticmethod
    def get_duration_text(duration):
        text = ""
        hours = 0
        minutes = 0

        tmp = duration

        if tmp > (60 * 60):
            hours = tmp // (60 * 60)
            tmp = tmp % (60 * 60)

        if tmp > 60:
            minutes = tmp // 60
            tmp = tmp % 60

        seconds = tmp

        if hours > 0:
            text += str(hours) + "H "

        if minutes > 0 or (seconds > 0 and hours > 0):
            text += str(minutes) + "M "

        if seconds > 0:
            text += str(seconds) + "S"

        return text.strip()

    def get_internal_name(self):
        return self.name.lower()

    def get_description(self, id_field_width=1, duration_field_width=1):
        current_epoch = time.time()

        about = self.get_name(True, True, id_field_width, duration_field_width) + " :: "

        if current_epoch < self.start_epoch:
            about += "Starts In: "
            about += self.get_duration_text(self.start_epoch - current_epoch)
        else:
            about += "Ends In: "
            about += self.get_duration_text(self.end_epoch - current_epoch)

        return about

    def get_description_with_channel(self, id_field_width=1, duration_field_width=1):
        return self.get_description(id_field_width, duration_field_width) + " :: " + self.channel

    def get_id(self):
        formatter = string.Formatter()
        return formatter.format("%d-%d", self.year, self.war_id)

    def get_chain_id(self):
        formatter = string.Formatter()
        chain = self.current_chain
        if self.state == WarState.FINISHED:
            chain -= 1

        return formatter.format("%s-%d", self.get_id(), chain)
