from enum import Enum


class WarState(Enum):
    PENDING = 1
    ACTIVE = 2
    CANCELLED = 3
    FINISHED = 4
