from irc.dict import IRCDict

from timmy.core import bot_instance
from timmy import db_access


class UserPerms:
    def __init__(self):
        self.admin_data_loaded = False
        self.admins = IRCDict()

    def _load_admin_data(self):
        select_statement = "SELECT * FROM `admins`"

        connection = db_access.connection_pool.get_connection()
        cursor = connection.cursor(dictionary=True)
        cursor.execute(select_statement)

        for row in cursor:
            self.admins[row['name']] = True

        self.admin_data_loaded = True

    def is_admin(self, user, channel):
        if user in bot_instance.channels[channel].opers():
            return True

        if not self.admin_data_loaded:
            self._load_admin_data()

        return user in self.admins or channel in self.admins

    @staticmethod
    def is_registered(user):
        user_data = db_access.user_directory.nick_directory.get(user)
        return user_data.registration_data_retrieved if user_data is not None else False
