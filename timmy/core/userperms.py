from irc.dict import IRCDict

from timmy import core, db_access


class UserPerms:
    def __init__(self):
        self.admin_data_loaded = False
        self.admins = IRCDict()

        self.ignore_data_loaded = False
        self.soft_ignores = IRCDict()
        self.hard_ignores = IRCDict()
        self.admin_ignores = IRCDict()

    def _load_admin_data(self) -> None:
        select_statement = "SELECT * FROM `admins`"

        connection = db_access.connection_pool.get_connection()
        cursor = connection.cursor(dictionary = True)
        cursor.execute(select_statement)

        self.admins.clear()

        for row in cursor:
            self.admins[row['name']] = True

        self.admin_data_loaded = True

        connection.close()

    def _load_ignore_data(self) -> None:
        select_statement = "SELECT `name`, `type` FROM `ignores`"

        connection = db_access.connection_pool.get_connection()
        cursor = connection.cursor(dictionary = True)
        cursor.execute(select_statement)

        self.soft_ignores.clear()
        self.hard_ignores.clear()

        for row in cursor:
            if row['type'] == 'soft':
                self.soft_ignores[row['name']] = True
            elif row['type'] == 'hard':
                self.hard_ignores[row['name']] = True
            elif row['type'] == 'admin':
                self.admin_ignores[row['name']] = True

        self.ignore_data_loaded = True

        connection.close()

    def is_admin(self, nick: str, channel: str) -> bool:
        if nick in core.bot_instance.channels[channel].opers():
            return True

        if not self.admin_data_loaded:
            self._load_admin_data()

        user_data = db_access.user_directory.find_user_data(nick)

        return user_data.global_admin or nick in self.admins  # or channel in self.admins

    @staticmethod
    def is_registered(nick: str) -> bool:
        user_data = db_access.user_directory.nick_directory.get(nick)
        return user_data.registration_data_retrieved if user_data is not None else False

    def is_ignored(self, nick: str, ignore_type: str = 'soft') -> bool:
        if not self.ignore_data_loaded:
            self._load_ignore_data()

        if ignore_type in ['soft', 'any'] and (nick in self.soft_ignores or nick in self.hard_ignores or nick in
                                               self.admin_ignores):
            return True

        if ignore_type == 'hard' and (nick in self.hard_ignores or nick in self.admin_ignores):
            return True

        if ignore_type == 'admin' and nick in self.admin_ignores:
            return True

        return False

    def add_ignore(self, nick: str, ignore_type: str) -> None:
        insert_statement = "INSERT INTO `ignores` SET `name` = %(nick)s, `type` = %(type)s"

        connection = db_access.connection_pool.get_connection()
        cursor = connection.cursor()

        cursor.execute(insert_statement, {'nick': nick, 'type': ignore_type})

        if ignore_type == 'soft':
            self.soft_ignores[nick] = True
        elif ignore_type == 'hard':
            self.hard_ignores[nick] = True
        elif ignore_type == 'admin':
            self.admin_ignores[nick] = True

    def remove_ignore(self, nick: str, ignore_type: str) -> None:
        insert_statement = "DELETE FROM `ignores` WHERE `name` = %(nick)s AND `type` = %(type)s"

        connection = db_access.connection_pool.get_connection()
        cursor = connection.cursor()

        cursor.execute(insert_statement, {'nick': nick, 'type': ignore_type})

        if ignore_type == 'soft' and nick in self.soft_ignores:
            del self.soft_ignores[nick]
        elif ignore_type == 'hard' and nick in self.hard_ignores:
            del self.hard_ignores[nick]
        elif ignore_type == 'admin' and nick in self.admin_ignores:
            del self.admin_ignores[nick]
