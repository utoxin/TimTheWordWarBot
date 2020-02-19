from timmy.data.command_type import CommandType


class CommandData:
    def __init__(self):
        self.type = CommandType.UNKNOWN
        self.issuer = ""
        self.command = ""
        self.args = []
        self.argstring = ""

    def __str__(self):
        return "Command: %s, Issuer: %s, Args: %s".format(self.command, self.issuer, self.argstring)
