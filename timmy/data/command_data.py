from timmy.data.command_type import CommandType


class CommandData:
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

    def __str__(self):
        return "Command: %s, Issuer: %s, Args: %s".format(self.prefix + self.command, self.issuer, self.arg_string)
