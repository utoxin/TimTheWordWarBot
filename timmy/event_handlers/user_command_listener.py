import re


class UserCommandListener:
    def __init__(self):
        self.user_command_regex = re.compile('^!(?!skynet)(\S+)\s?(.*?)$', re.IGNORECASE)
        self.admin_command_regex = re.compile('^\$(?!skynet)(\S+)\s?(.*?)$', re.IGNORECASE)

    def on_pubmsg(self, connection, event):
