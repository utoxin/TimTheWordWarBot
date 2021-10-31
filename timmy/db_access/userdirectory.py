import copy
import uuid
from datetime import datetime, timedelta
from typing import Optional

from irc.dict import IRCDict

from timmy import core, db_access
from timmy.data.userdata import UserData


class UserDirectory:
    def __init__(self):
        self.user_data_loaded = False

        self.auth_directory = IRCDict()
        self.nick_directory = IRCDict()
        self.temp_directory = IRCDict()

    def _do_initial_db_load(self) -> None:
        select_statement = "SELECT * FROM `users`"

        connection = db_access.connection_pool.get_connection()
        cursor = connection.cursor(dictionary = True)
        cursor.execute(select_statement)

        for row in cursor:
            user = UserData()
            user.uuid = uuid.UUID(row['id'])
            user.authed_user = row['authed_user']
            user.global_admin = bool(row['global_admin'])
            user.total_sprint_wordcount = row['total_sprint_wordcount']
            user.total_sprints = row['total_sprints']
            user.total_sprint_duration = row['total_sprint_duration']
            user.raptor_adopted = bool(row['raptor_adopted'])
            user.raptor_name = row['raptor_name']
            user.raptor_favorite_color = row['raptor_favorite_color']
            user.raptor_bunnies_stolen = row['raptor_bunnies_stolen']
            user.last_bunny_raid = row['last_bunny_raid']
            user.registration_data_retrieved = True

            self.auth_directory[user.authed_user] = user
            self.nick_directory[user.authed_user] = user

        self.user_data_loaded = True

        db_access.connection_pool.close_connection(connection)

    def find_user_data(self, nick: str, include_temp_data: bool = False) -> Optional[UserData]:
        if not self.user_data_loaded:
            self._do_initial_db_load()

        if nick in self.auth_directory:
            return self.auth_directory[nick]

        if nick in self.nick_directory:
            return self.nick_directory[nick]

        if include_temp_data and nick in self.temp_directory:
            return self.temp_directory[nick]

        self.send_whois(nick)

        return None

    @staticmethod
    def send_whois(nick: str) -> None:
        core.bot_instance.connection.whois(nick)

    def cleanup_temp_list(self) -> None:
        local_copy = copy.deepcopy(self.temp_directory)

        for nick, user_data in local_copy.items():
            time_difference = datetime.now() - user_data.last_whois_check
            if not user_data.registration_data_retrieved and time_difference > timedelta(hours = 1):
                # TODO: Add irc-logging call :: "Removing stale temp user: NICK"
                del self.temp_directory[nick]
