from irc.dict import IRCDict

from timmy import db_access, core


class UserPerms:
    def __init__(self):
        self.admin_data_loaded = False
        self.admins = IRCDict()

        self.ignore_data_loaded = False
        self.soft_ignores = IRCDict()
        self.hard_ignores = IRCDict()

    def _load_admin_data(self):
        select_statement = "SELECT * FROM `admins`"

        connection = db_access.connection_pool.get_connection()
        cursor = connection.cursor(dictionary=True)
        cursor.execute(select_statement)

        self.admins.clear()

        for row in cursor:
            self.admins[row['name']] = True

        self.admin_data_loaded = True

    def _load_ignore_data(self):
        select_statement = "SELECT `name`, `type` FROM `ignores`"

        connection = db_access.connection_pool.get_connection()
        cursor = connection.cursor(dictionary=True)
        cursor.execute(select_statement)

        self.soft_ignores.clear()
        self.hard_ignores.clear()

        for row in cursor:
            if row['type'] == 'soft':
                self.soft_ignores[row['name']] = True
            elif row['type'] == 'hard':
                self.hard_ignores[row['name']] = True

        self.ignore_data_loaded = True

    def is_admin(self, nick, channel):
        if nick in core.bot_instance.channels[channel].opers():
            return True

        if not self.admin_data_loaded:
            self._load_admin_data()

        return nick in self.admins or channel in self.admins

    @staticmethod
    def is_registered(nick):
        user_data = db_access.user_directory.nick_directory.get(nick)
        return user_data.registration_data_retrieved if user_data is not None else False

    def is_ignored(self, nick, ignore_type='soft'):
        if not self.ignore_data_loaded:
            self._load_ignore_data()

        if ignore_type in ['soft', 'any'] and nick in self.soft_ignores:
            return True

        if ignore_type in ['hard', 'any'] and nick in self.hard_ignores:
            return True

        return False
