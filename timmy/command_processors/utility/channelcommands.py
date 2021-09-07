import time

from irc.client import Event, ServerConnection

from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData


class ChannelCommands(BaseCommand):
    admin_commands = {'automuzzlewars', 'muzzle', 'chatterlevel', 'chatterflag', 'commandflag',
                      'twitterrelay', 'twitterbucket', 'part', 'unmuzzle'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        from timmy.core import user_perms
        if not user_perms.is_admin(command_data.issuer, command_data.channel):
            self.respond_to_user(connection, event, "I'm sorry, only admins are allowed to do that.")
            return

        try:
            command_handler = getattr(self, '_' + command_data.command + '_handler')
            command_handler(connection, event, command_data)
        except AttributeError:
            self.respond_to_user(connection, event, "That command is not yet implemented.")

    def _validate_channel(self, connection: ServerConnection, event: Event, candidate_channel, user) -> bool:
        from timmy.core import user_perms, bot_instance

        if candidate_channel not in bot_instance.channels:
            self.respond_to_user(connection, event, "I don't know about '{}'. Sorry.".format(candidate_channel))
            return False

        if not user_perms.is_admin(user, candidate_channel):
            self.respond_to_user(
                    connection, event, "I'm sorry, you aren't an admin "
                                       "for '{}'.".format(candidate_channel)
            )
            return False

        return True

    def _unmuzzle_handler(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        from timmy.core import bot_instance

        if command_data.arg_count == 0:
            target = command_data.channel
        else:
            if not self._validate_channel(connection, event, command_data.args[0], command_data.issuer):
                return

            target = command_data.args[0]

        bot_instance.channels[target].set_muzzle_flag(False, 0)

        self.respond_to_user(connection, event, "Channel unmuzzled.")
        return

    def _part_handler(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if command_data.arg_count == 0:
            target = command_data.channel
        else:
            if not self._validate_channel(connection, event, command_data.args[0], command_data.issuer):
                return

            target = command_data.args[0]

        connection.part([target], "Bye!")

        if target != command_data.channel:
            self.respond_to_user(connection, event, "Channel left.")

        return

    def _muzzle_handler(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        from timmy.core import bot_instance

        if command_data.arg_count == 0:
            self.respond_to_user(connection, event, "Usage: $muzzle [#channel] <duration in minutes>")
            self.respond_to_user(connection, event, "Usage: Duration of -1 means indefinite.")
            self.respond_to_user(connection, event, "Example: $muzzle 10  --  Will muzzle Timmy for 10 minutes.")
            return

        if command_data.arg_count == 1:
            target = command_data.channel
            duration = float(command_data.args[0])
        else:
            if not self._validate_channel(connection, event, command_data.args[0], command_data.issuer):
                return

            target = command_data.args[0].lower()
            duration = float(command_data.args[1])

        if duration < 0:
            expiration = None
        else:
            expiration = time.time() + duration * 60

        bot_instance.channels[target].set_muzzle_flag(True, expiration)

        self.respond_to_user(connection, event, "Channel muzzled for specified time.")
        return

    def _automuzzlewars_handler(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        from timmy.core import bot_instance

        if command_data.arg_count == 0:
            self.respond_to_user(connection, event, "Usage: $automuzzlewars [#channel] <0/1>")
            self.respond_to_user(connection, event, "Usage: Whether Timmy should auto-muzzle during word wars.")
            self.respond_to_user(connection, event, "Usage: 0 disables this feature, 1 enables it.")
            self.respond_to_user(
                    connection, event, "Example: $automuzzlewars 1  --  Enables automuzzle for this "
                                       "channel"
            )
            return

        if command_data.arg_count == 1:
            target = command_data.channel
            flag = int(command_data.args[0]) == 1
        else:
            if not self._validate_channel(connection, event, command_data.args[0], command_data.issuer):
                return

            target = command_data.args[0].lower()
            flag = int(command_data.args[1]) == 1

        bot_instance.channels[target].set_muzzle_flag(flag)

        self.respond_to_user(connection, event, "Channel auto muzzle flag updated for {}.".format(target))
        return

    def _chatterlevel_handler(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        from timmy.core import bot_instance

        def output_usage():
            self.respond_to_user(connection, event, "Usage: $chatterlevel <#channel> list")
            self.respond_to_user(
                    connection, event, "Usage: $chatterlevel <#channel> set <%/Msg> <Name Multiplier> "
                                       "<%/Min>"
            )

        if command_data.arg_count == 1 or command_data.arg_count == 2:
            if command_data.arg_count == 1:
                channel = command_data.channel
                command = command_data.args[0].lower()
            else:
                channel = command_data.args[0].lower()
                command = command_data.args[1].lower()

                if not self._validate_channel(connection, event, channel, command_data.issuer):
                    return

            if command == 'list':
                from timmy.data.channel_data import ChannelData
                channel_data: ChannelData = bot_instance.channels[channel]

                reactive = channel_data.chatter_settings['reactive_level']
                random = channel_data.chatter_settings['random_level']
                name = channel_data.chatter_settings['name_multiplier']

                self.respond_to_user(
                        connection, event, f"Reactive Chatter Level: {reactive:.3f}%/Msg - "
                                           f"Name Multiplier: {name:.3f}"
                )
                self.respond_to_user(connection, event, f"Random Chatter Level: {random:.3f}%/Min")
            else:
                output_usage()
                return

        elif command_data.arg_count == 4 or command_data.arg_count == 5:
            if command_data.arg_count == 4:
                channel = command_data.channel
                command = command_data.args[0].lower()
                reactive = float(command_data.args[1])
                name = float(command_data.args[2])
                random = float(command_data.args[3])
            else:
                channel = command_data.args[0].lower()
                command = command_data.args[1].lower()
                reactive = float(command_data.args[2])
                name = float(command_data.args[3])
                random = float(command_data.args[4])

                if not self._validate_channel(connection, event, channel, command_data.issuer):
                    return

            if command == 'set':
                from timmy.data.channel_data import ChannelData
                channel_data: ChannelData = bot_instance.channels[channel]

                reactive = max(min(reactive, 100), 0)
                name = max(min(name, 100), 0)
                random = max(min(random, 100), 0)

                channel_data.set_chatter_flags(reactive, name, random)

                self.respond_to_user(
                        connection, event, f"Reactive Chatter Level: {reactive:.3f}%/Msg - "
                                           f"Name Multiplier: {name:.3f}"
                )
                self.respond_to_user(connection, event, f"Random Chatter Level: {random:.3f}%/Min")
            else:
                output_usage()
                return

        else:
            output_usage()
            return

        return

    def _chatterflag_handler(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        from timmy.core import bot_instance

        if 1 <= command_data.arg_count <= 2 and command_data.args[command_data.arg_count - 1].lower() == 'list':
            if command_data.arg_count == 1:
                channel = command_data.channel
            else:
                channel = command_data.args[0].lower()

            if not self._validate_channel(connection, event, channel, command_data.issuer):
                return

            channel_data: ChannelData = bot_instance.channels[channel]

            self.respond_to_user(
                    connection, event, f"Sending status of chatter settings for {channel} via private "
                                       f"message."
            )

            for key, value in channel_data.chatter_settings['types'].items():
                connection.privmsg(command_data.issuer, f"{key}: {value}")

        elif 3 <= command_data.arg_count <= 4 and command_data.args[command_data.arg_count - 3].lower() == 'set':
            if command_data.arg_count == 3:
                channel = command_data.channel
                flag = command_data.args[1].lower()
                value = int(command_data.args[2]) == 1
            else:
                channel = command_data.args[0].lower()
                flag = command_data.args[2].lower()
                value = int(command_data.args[3]) == 1

            if not self._validate_channel(connection, event, channel, command_data.issuer):
                return

            channel_data: ChannelData = bot_instance.channels[channel]

            if flag == 'all':
                for key, value in channel_data.chatter_settings['types'].items():
                    channel_data.chatter_settings['types'][key] = value

                channel_data.save_data()

                self.respond_to_user(connection, event, f"All chatter flags updated.")
            else:
                if flag in channel_data.chatter_settings['types']:
                    channel_data.chatter_settings['types'][flag] = value
                    channel_data.save_data()
                    self.respond_to_user(connection, event, f"Chatter flag updated.")
                else:
                    self.respond_to_user(connection, event, f"I'm sorry, but I don't have a setting for {flag}")

        else:
            flags = ", ".join(ChannelData.chatter_settings_defaults['types'].keys())

            self.respond_to_user(
                    connection, event, "Usage: $chatterflag [<#channel>] list OR "
                                       "$chatterflag [<#channel>] set <type> <0/1>"
            )
            self.respond_to_user(connection, event, f"Valid Chatter Types: all, {flags}")

    def _commandflag_handler(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        from timmy.core import bot_instance

        if 1 <= command_data.arg_count <= 2 and command_data.args[command_data.arg_count - 1].lower() == 'list':
            if command_data.arg_count == 1:
                channel = command_data.channel
            else:
                channel = command_data.args[0].lower()

            if not self._validate_channel(connection, event, channel, command_data.issuer):
                return

            channel_data: ChannelData = bot_instance.channels[channel]

            self.respond_to_user(
                    connection, event, f"Sending status of command settings for {channel} via private "
                                       f"message."
            )

            for key, value in channel_data.command_settings.items():
                connection.privmsg(command_data.issuer, f"{key}: {value}")

        elif 3 <= command_data.arg_count <= 4 and command_data.args[command_data.arg_count - 3].lower() == 'set':
            if command_data.arg_count == 3:
                channel = command_data.channel
                command = command_data.args[1].lower()
                value = int(command_data.args[2]) == 1
            else:
                channel = command_data.args[0].lower()
                command = command_data.args[2].lower()
                value = int(command_data.args[3]) == 1

            if not self._validate_channel(connection, event, channel, command_data.issuer):
                return

            channel_data: ChannelData = bot_instance.channels[channel]

            if command == 'all':
                for key, value in channel_data.command_settings.items():
                    channel_data.command_settings[key] = value

                channel_data.save_data()

                self.respond_to_user(connection, event, f"All command flags updated.")
            else:
                if command in channel_data.command_settings:
                    channel_data.command_settings[command] = value
                    channel_data.save_data()
                    self.respond_to_user(connection, event, f"Command flag updated.")
                else:
                    self.respond_to_user(connection, event, f"I'm sorry, but I don't have a setting for {command}")

        else:
            commands = ", ".join(ChannelData.command_defaults.keys())

            self.respond_to_user(
                    connection, event, "Usage: $commandflag [<#channel>] list OR "
                                       "$commandflag [<#channel>] set <type> <0/1>"
            )
            self.respond_to_user(connection, event, f"Valid Command Types: all, {commands}")
