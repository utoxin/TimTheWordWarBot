from irc.dict import IRCDict
from irc.strings import IRCFoldedCase

from timmy import db_access, core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData
from timmy.data.userdata import UserData
from timmy.db_access import user_directory


class InteractionControls(BaseCommand):
    user_commands = {'interactionflag'}

    def __init__(self):
        self.interaction_settings = IRCDict()
        self.initialized = False
        self.interactions = set()

    def init(self):
        if not self.initialized:
            for entry in ChannelData.chatter_settings_defaults['types'].keys():
                self.interactions.add(entry)

            for entry in ChannelData.command_defaults.keys():
                self.interactions.add(entry)

            self.interactions.add('hugs')
            self.interactions.remove('bored')
            self.interactions.remove('chainstory')
            self.interactions.remove('commandment')
            self.interactions.remove('dance')
            self.interactions.remove('dice')
            self.interactions.remove('eightball')
            self.interactions.remove('expound')
            self.interactions.remove('markov')
            self.interactions.remove('ping')
            self.interactions.remove('sing')
            self.interactions.remove('woot')

            self._load_interaction_settings()

            self.initialized = True

    def process(self, connection, event, command_data: CommandData):
        self.init()

        if core.user_perms.is_registered(command_data.issuer):
            target: UserData = user_directory.find_user_data(command_data.issuer)

            if command_data.arg_count == 1 and command_data.args[0] == 'list':
                if target.authed_user in self.interaction_settings:
                    data: dict = self.interaction_settings[target.authed_user]

                    if not command_data.in_pm:
                        self.respond_to_user(connection, event, "Sending status of interaction settings via private "
                                                                "message.")

                    for key, value in data.items():
                        connection.privmsg(command_data.issuer, "{}: {}".format(key, value))
                else:
                    self.respond_to_user(connection, event, "No settings stored for that user.")

            elif command_data.arg_count == 3 and command_data.args[0].lower() == 'set':
                if target.authed_user not in self.interaction_settings:
                    self.interaction_settings[target.authed_user] = IRCDict()

                data = self.interaction_settings[target.authed_user]
                flag = '1' == command_data.args[2]

                if command_data.args[1].lower() == 'all':
                    for key in self.interactions:
                        data[key] = flag

                    self._save_interaction_settings()

                    self.respond_to_user(connection, event, "All interaction flags updated.")
                else:
                    if command_data.args[1] in self.interactions:
                        # We only need to store disabled interactions. Default is enabled.
                        if not flag:
                            data[command_data.args[1]] = flag
                        else:
                            del(data[command_data.args[1]])

                        self._save_interaction_settings()

                        self.respond_to_user(connection, event, "Interaction flag updated.")
                    else:
                        self.respond_to_user(connection, event, "I'm sorry, but I don't have a setting for {}".format(
                                command_data.args[1]))
            else:
                self.respond_to_user(connection, event, "Usage: !interactionflag list OR !interactionflag set <type> "
                                                        "<0/1>")
                self.respond_to_user(connection, event, "Valid interaction types: all, {}".format(
                        ", ".join(self.interactions)))
        else:
            self.respond_to_user(connection, event, "You must be logged in with NickServ to use these commands.")

        return True

    def interact_with_user(self, username, interaction):
        self.init()

        target: UserData = user_directory.find_user_data(username)

        if target is None or target.authed_user not in self.interaction_settings:
            return True

        if interaction not in self.interaction_settings[target.authed_user]:
            return True

        return self.interaction_settings[target.authed_user][interaction]

    def _save_interaction_settings(self):
        truncate_statement = "TRUNCATE `user_interaction_settings`"
        insert_statement = "INSERT INTO `user_interaction_settings` SET `username` = %(username)s, " \
                           "`setting` = %(setting)s, `value` = %(value)s"

        conn = db_access.connection_pool.get_connection()
        cursor = conn.cursor()
        cursor.execute(truncate_statement)

        for user, settings in self.interaction_settings.items():
            for setting, value in settings.items():
                cursor.execute(insert_statement, {
                    'username': "{}".format(user),
                    'setting': "{}".format(setting),
                    'value': value == 1
                })

        conn.close()

    def _load_interaction_settings(self):
        select_statement = "SELECT `username`, `setting`, `value` FROM `user_interaction_settings`"

        conn = db_access.connection_pool.get_connection()
        cursor = conn.cursor(dictionary=True)
        cursor.execute(select_statement)

        for row in cursor:
            if row['username'] not in self.interaction_settings:
                self.interaction_settings[row['username']] = IRCDict()

            self.interaction_settings[row['username']][row['setting']] = row['value'] == 1

        conn.close()
