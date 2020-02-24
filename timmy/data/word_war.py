import time
import uuid
from datetime import datetime

import string

from timmy import db_access
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
        self.war_members = set()
        self.db_access = db_access.word_war_db

    def basic_setup(self, channel, starter, name, base_duration, start_epoch):
        self.uuid = uuid.uuid4()
        self.channel = channel
        self.starter = starter
        self.name = name
        self.base_duration = base_duration
        self.start_epoch = start_epoch
        self.end_epoch = start_epoch + base_duration
        self.total_chains = 1
        self.current_chain = 1
        self.randomness = False
        self.state = WarState.PENDING
        self.year = datetime.today().year

        self.db_access.create_war(self)

    def advanced_setup(self, channel, starter, name, base_duration, start_epoch, total_chains, base_break, randomness):
        self.uuid = uuid.uuid4()
        self.channel = channel
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
        self.year = datetime.today().year

        self.db_access.create_war(self)

    def load_from_db(self, row):
        self.uuid = uuid.UUID(row['uuid'])
        self.year = int(row['year'])
        self.war_id = int(row['war_id'])
        self.channel = row['channel']
        self.starter = row['starter']
        self.name = row['name']
        self.base_duration = int(row['base_duration'])
        self.base_break = int(row['base_break'])
        self.total_chains = int(row['total_chains'])
        self.current_chain = int(row['current_chain'])
        self.start_epoch = int(row['start_epoch'])
        self.end_epoch = int(row['end_epoch'])
        self.randomness = bool(row['randomness'])
        self.state = WarState[row['war_state']]

    def data_export(self):
        data = vars(self)
        data['uuid'] = str(data['uuid'])
        data['state'] = data['state'].name
        del data['war_members']
        del data['db_access']
        return data

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
        self.db_access.update_war(self)

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
