import uuid
from datetime import datetime, timedelta

from timmy import core
from timmy.data.userdata import UserData
from timmy.db_access import user_directory
from timmy.utilities import irc_logger


class WhoisHandler:
    def on_action(self, connection, event):
        if event.source.nick is not None and event.source.nick != core.bot_instance.connection.nickname:
            self._handle_nick_event(event.source.nick)

    def on_join(self, connection, event):
        if event.source.nick is not None and event.source.nick != core.bot_instance.connection.nickname:
            self._handle_nick_event(event.source.nick)

    def on_privmsg(self, connection, event):
        if event.source.nick is not None and event.source.nick != core.bot_instance.connection.nickname:
            self._handle_nick_event(event.source.nick)

    def on_pubmsg(self, connection, event):
        if event.source.nick is not None and event.source.nick != core.bot_instance.connection.nickname:
            self._handle_nick_event(event.source.nick)

    def on_nick(self, connection, event):
        if event.source.nick is not None and event.source.nick != core.bot_instance.connection.nickname:
            old_nick = event.source.nick
            new_nick = event.target

            if old_nick in user_directory.nick_directory:
                data = user_directory.nick_directory.get(old_nick)
                user_directory.nick_directory[new_nick] = data
                del user_directory.nick_directory[old_nick]

            if old_nick in user_directory.temp_directory:
                data = user_directory.temp_directory.get(old_nick)
                user_directory.temp_directory[new_nick] = data
                del user_directory.temp_directory[old_nick]

            self._handle_nick_event(new_nick)

    def on_namreply(self, connection, event):
        for nick in event.arguments[2].split():
            if nick[0] in core.bot_instance.connection.features.prefix:
                nick = nick[1:]

            self._handle_nick_event(nick)

    @staticmethod
    def on_whoisaccount(connection, event):
        nick = event.arguments[0]

        if event.arguments[2] == 'is logged in as':
            authed_as = event.arguments[1]
            irc_logger.log_message("Received whois response. Nick: {} :: Registered As: {}".format(
                    nick,
                    authed_as
            ))

            auth_data = user_directory.auth_directory.get(authed_as)
            nick_data = user_directory.nick_directory.get(nick)
            if auth_data is not None:
                if nick_data is None:
                    irc_logger.log_message("Adding {} to existing user {}".format(
                            nick,
                            auth_data.uuid
                    ))

                    user_directory.nick_directory[nick] = auth_data
                    auth_data.nicks.add(nick)
                    auth_data.save()
            else:
                temp_data = user_directory.temp_directory.get(nick)

                if temp_data is not None:
                    irc_logger.log_message("Moving {} to permanent user {}".format(
                            nick,
                            temp_data.uuid
                    ))

                    temp_data.registration_data_retrieved = True
                    temp_data.authed_user = authed_as
                    temp_data.nicks.add(nick)
                else:
                    irc_logger.log_message("Adding {} as entirely new user. How did this happen?".format(nick))

                    temp_data = UserData()
                    temp_data.uuid = uuid.uuid4()
                    temp_data.authed_user = authed_as
                    temp_data.nicks.add(nick)
                    temp_data.registration_data_retrieved = True

                temp_data.save()

                user_directory.auth_directory[authed_as] = temp_data
                for nick_entry in temp_data.nicks:
                    user_directory.nick_directory[nick_entry] = temp_data

                if nick in user_directory.temp_directory:
                    del user_directory.temp_directory[nick]
        else:
            irc_logger.log_message("Received whois response. Nick: {} :: Not Registered".format(nick))

    @staticmethod
    def _handle_nick_event(nick):
        user_data = user_directory.find_user_data(nick, True)

        if user_data is None:
            user_data = UserData()
            user_data.uuid = uuid.uuid4()
            user_data.nicks.add(nick)
            user_data.last_whois_check = datetime.now()

            user_directory.temp_directory[nick] = user_data
            irc_logger.log_message("Sending initial whois request for {}".format(nick))
            user_directory.send_whois(nick)

        else:
            if not user_data.registration_data_retrieved:
                if user_data.last_whois_check is not None:
                    time_difference = datetime.now() - user_data.last_whois_check
                else:
                    time_difference = timedelta(minutes=60)

                if time_difference > timedelta(minutes=10):
                    user_data.last_whois_check = datetime.now()
                    irc_logger.log_message("Sending another whois request for {}".format(nick))
                    user_directory.send_whois(nick)

        user_directory.cleanup_temp_list()
