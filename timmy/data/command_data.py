from typing import Optional

from timmy.data.command_type import CommandType
from timmy.data.userdata import UserData


class CommandData:
    type: CommandType
    issuer_data: Optional[UserData]
    issuer: str
    channel: str
    command: str
    args: list
    arg_count: int
    arg_string: str
    prefix: str
    in_pm: bool
    automatic: bool

    def __init__(self):
        self.type = CommandType.UNKNOWN
        self.issuer = ""
        self.channel = ""
        self.command = ""
        self.args = []
        self.arg_count = 0
        self.arg_string = ""
        self.prefix = ""
        self.in_pm = False
        self.automatic = False

    def __str__(self):
        return "Command: %s, Issuer: %s, Args: %s".format(self.prefix + self.command, self.issuer, self.arg_string)
