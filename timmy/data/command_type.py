from enum import Enum


class CommandType(Enum):
    TIMMY_USER = 1
    TIMMY_ADMIN = 2
    SKYNET_USER = 3
    SKYNET_ADMIN = 4
    UNKNOWN = 5
