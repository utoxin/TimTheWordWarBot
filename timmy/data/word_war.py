import time
import uuid
from datetime import datetime
from typing import Dict, Optional, Set

from timmy import db_access
from timmy.data.war_state import WarState


class WordWar:
    year: int
    war_id: int
    channel: str
    starter: str
    name: str
    base_duration: float
    base_break: float
    total_chains: int
    current_chain: int
    start_epoch: float
    end_epoch: float
    randomness: bool
    start: Optional[WarState]
    war_members: Set[str]

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

        from timmy.db_access import WordWarDb
        self.db_access: WordWarDb = db_access.word_war_db

    def basic_setup(self, channel: str, starter: str, name: str, base_duration: float, start_epoch: float) -> None:
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

    def advanced_setup(
            self, channel: str, starter: str, name: str, base_duration: float, start_epoch: float, total_chains: int,
            base_break: float, randomness: bool
    ) -> None:
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

    def load_from_db(self, row: Dict[str, str]) -> None:
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

    def data_export(self) -> dict:
        data = {
            'uuid':          str(self.uuid),
            'year':          self.year,
            'war_id':        self.war_id,
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
            'state':         self.state.name
        }

        return data

    def end_war(self) -> None:
        self.state = WarState.FINISHED
        self.update_db()

    def cancel_war(self) -> None:
        self.state = WarState.CANCELLED
        self.update_db()

    def begin_war(self) -> None:
        self.state = WarState.ACTIVE
        self.update_db()

    def start_break(self) -> None:
        self.state = WarState.PENDING
        self.update_db()

    def update_db(self) -> None:
        self.db_access.update_war(self)

    def update_war_members(self) -> None:
        self.db_access.save_war_members(self)

    def duration(self) -> float:
        return self.end_epoch - self.start_epoch

    def get_name(
            self, include_id = False, include_duration = False, id_field_width = 1, duration_field_width = 1
    ) -> str:
        name_parts = []

        if include_id:
            db_id = self.get_id()
            name_parts.append(f"[ID {db_id:<{id_field_width:d}}]")

        if include_duration:
            duration_value = self.get_duration_text(self.duration())
            name_parts.append(f"[{duration_value:<{duration_field_width:d}}]")

        name_parts.append(self.name)

        if self.total_chains > 1:
            name_parts.append(f"({self.current_chain:d}/{self.total_chains:d})")

        return " ".join(name_parts)

    @staticmethod
    def get_duration_text(duration_value: float) -> str:
        text: str = ""
        hours: int = 0
        minutes: int = 0

        tmp: float = duration_value

        if tmp > (60 * 60):
            hours = int(tmp // (60 * 60))
            tmp = tmp % (60 * 60)

        if tmp > 60:
            minutes = int(tmp // 60)
            tmp = tmp % 60

        seconds: float = tmp

        if hours > 0:
            text += f"{hours:,.0f}H "

        if minutes > 0 or (seconds > 0 and hours > 0):
            text += f"{minutes:.0f}M "

        if seconds > 0:
            text += f"{seconds:.0f}S"

        return text.strip()

    def get_internal_name(self) -> str:
        return self.name.lower()

    def get_description(self, id_field_width: int = 1, duration_field_width: int = 1) -> str:
        current_epoch: float = time.time()

        about: str = self.get_name(True, True, id_field_width, duration_field_width) + " :: "

        if current_epoch < self.start_epoch:
            about += "Starts In: "
            about += self.get_duration_text(self.start_epoch - current_epoch)
        else:
            about += "Ends In: "
            about += self.get_duration_text(self.end_epoch - current_epoch)

        return about

    def get_description_with_channel(self, id_field_width: int = 1, duration_field_width: int = 1) -> str:
        return self.get_description(id_field_width, duration_field_width) + " :: " + self.channel

    def get_id(self) -> str:
        return f"{self.year:d}-{self.war_id:d}"

    def get_chain_id(self) -> str:
        chain = self.current_chain
        if self.state == WarState.FINISHED:
            chain -= 1

        return f"{self.get_id()}-{chain:d}"

    def add_member(self, member: str) -> None:
        self.war_members.add(member)
        self.update_war_members()

    def remove_member(self, member: str) -> None:
        self.war_members.remove(member)
        self.update_war_members()
