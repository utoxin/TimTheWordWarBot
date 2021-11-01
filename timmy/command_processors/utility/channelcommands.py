import time

from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData


class ChannelCommands(BaseCommand):
    admin_commands = ['automuzzlewars', 'muzzle', 'chatterlevel', 'chatterflag', 'commandflag',
                      'twitterrelay', 'twitterbucket', 'part', 'unmuzzle']

    help_topics = [('admin', 'channel commands', '$muzzle [#channel] <minutes>', 'Turns on the muzzle flag '
                                                                                 'temporarily. -1 is indefinite.'),
                   ('admin', 'channel commands', '$automuzzlewars [#channel] <0/1>', 'Whether to auto-muzzle during '
                                                                                     'word wars.'),
                   ('admin', 'channel commands', '$chatterlevel', 'Control the chatter level for Timmy.'),
                   ('admin', 'channel commands', '$chatterflag', 'Control what chatter types Timmy will use.'),
                   ('admin', 'channel commands', '$commandflag', 'Control what commands can be used in the channel.'),
                   ('admin', 'channel commands', '$part', 'Force Timmy to leave this channel.')]

    def process(self, command_data: CommandData) -> None:
        from timmy.core import user_perms
        if not command_data.in_pm and not user_perms.is_admin(command_data.issuer, command_data.channel):
            self.respond_to_user(command_data, "I'm sorry, only admins are allowed to do that.")
            return

        try:
            command_handler = getattr(self, '_' + command_data.command + '_handler')
            command_handler(command_data)
        except AttributeError:
            self.respond_to_user(command_data, "That command is not yet implemented.")

    def _unmuzzle_handler(self, command_data: CommandData) -> None:
        from timmy.core import bot_instance

        if command_data.arg_count == 0:
            target = command_data.channel
        else:
            if not self._is_channel_admin(command_data, command_data.args[0], command_data.issuer):
                return

            target = command_data.args[0]

        bot_instance.channels[target].set_muzzle_flag(False, 0)

        self.respond_to_user(command_data, "Channel unmuzzled.")
        return

    def _part_handler(self, command_data: CommandData) -> None:
        if command_data.arg_count == 0:
            target = command_data.channel
        else:
            if not self._is_channel_admin(command_data, command_data.args[0], command_data.issuer):
                return

            target = command_data.args[0]

        from timmy.core import bot_instance
        bot_instance.connection.part([target], "Bye!")

        if target != command_data.channel:
            self.respond_to_user(command_data, "Channel left.")

        channel_data: ChannelData = bot_instance.channels[target]

        from timmy.db_access import channel_db
        channel_db.deactivate_channel(channel_data)

        return

    def _muzzle_handler(self, command_data: CommandData) -> None:
        from timmy.core import bot_instance

        if command_data.arg_count == 0:
            self.respond_to_user(command_data, "Usage: $muzzle [#channel] <duration in minutes>")
            self.respond_to_user(command_data, "Usage: Duration of -1 means indefinite.")
            self.respond_to_user(command_data, "Example: $muzzle 10  --  Will muzzle Timmy for 10 minutes.")
            return

        if command_data.arg_count == 1:
            target = command_data.channel
            duration = float(command_data.args[0])
        else:
            if not self._is_channel_admin(command_data, command_data.args[0], command_data.issuer):
                return

            target = command_data.args[0].lower()
            duration = float(command_data.args[1])

        if duration < 0:
            expiration = None
        else:
            expiration = time.time() + duration * 60

        bot_instance.channels[target].set_muzzle_flag(True, expiration)

        self.respond_to_user(command_data, "Channel muzzled for specified time.")
        return

    def _automuzzlewars_handler(self, command_data: CommandData) -> None:
        from timmy.core import bot_instance

        if command_data.arg_count == 0:
            self.respond_to_user(command_data, "Usage: $automuzzlewars [#channel] <0/1>")
            self.respond_to_user(command_data, "Usage: Whether Timmy should auto-muzzle during word wars.")
            self.respond_to_user(command_data, "Usage: 0 disables this feature, 1 enables it.")
            self.respond_to_user(command_data, "Example: $automuzzlewars 1  --  Enables automuzzle for this channel")
            return

        if command_data.arg_count == 1:
            target = command_data.channel
            flag = int(command_data.args[0]) == 1
        else:
            if not self._is_channel_admin(command_data, command_data.args[0], command_data.issuer):
                return

            target = command_data.args[0].lower()
            flag = int(command_data.args[1]) == 1

        bot_instance.channels[target].set_auto_muzzle(flag)

        self.respond_to_user(command_data, "Channel auto muzzle flag updated for {}.".format(target))
        return

    def _chatterlevel_handler(self, command_data: CommandData) -> None:
        from timmy.core import bot_instance

        def output_usage():
            self.respond_to_user(command_data, "Usage: $chatterlevel <#channel> list")
            self.respond_to_user(command_data, "Usage: $chatterlevel <#channel> set <%/Msg> <Name Multiplier> <%/Min>")

        if command_data.arg_count == 1 or command_data.arg_count == 2:
            if command_data.arg_count == 1:
                channel = command_data.channel
                command = command_data.args[0].lower()
            else:
                channel = command_data.args[0].lower()
                command = command_data.args[1].lower()

                if not self._is_channel_admin(command_data, channel, command_data.issuer):
                    return

            if command == 'list':
                from timmy.data.channel_data import ChannelData
                channel_data: ChannelData = bot_instance.channels[channel]

                reactive = channel_data.chatter_settings['reactive_level']
                random = channel_data.chatter_settings['random_level']
                name = channel_data.chatter_settings['name_multiplier']

                self.respond_to_user(command_data, f"Reactive Chatter Level: {reactive:.3f}%/Msg - Name Multiplier: "
                                                   f"{name:.3f}")
                self.respond_to_user(command_data, f"Random Chatter Level: {random:.3f}%/Min")
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

                if not self._is_channel_admin(command_data, channel, command_data.issuer):
                    return

            if command == 'set':
                from timmy.data.channel_data import ChannelData
                channel_data: ChannelData = bot_instance.channels[channel]

                reactive = max(min(reactive, 100), 0)
                name = max(min(name, 100), 0)
                random = max(min(random, 100), 0)

                channel_data.set_chatter_flags(reactive, name, random)

                self.respond_to_user(command_data, f"Reactive Chatter Level: {reactive:.3f}%/Msg - Name Multiplier: "
                                                   f"{name:.3f}")
                self.respond_to_user(command_data, f"Random Chatter Level: {random:.3f}%/Min")
            else:
                output_usage()
                return

        else:
            output_usage()
            return

        return

    def _chatterflag_handler(self, command_data: CommandData) -> None:
        from timmy.core import bot_instance

        if 1 <= command_data.arg_count <= 2 and command_data.args[command_data.arg_count - 1].lower() == 'list':
            if command_data.arg_count == 1:
                channel = command_data.channel
            else:
                channel = command_data.args[0].lower()

            if not self._is_channel_admin(command_data, channel, command_data.issuer):
                return

            channel_data: ChannelData = bot_instance.channels[channel]

            self.respond_to_user(command_data, f"Sending status of chatter settings for {channel} via private message.")

            for key, value in channel_data.chatter_settings['types'].items():
                bot_instance.connection.privmsg(command_data.issuer, f"{key}: {value}")

        elif 3 <= command_data.arg_count <= 4 and command_data.args[command_data.arg_count - 3].lower() == 'set':
            if command_data.arg_count == 3:
                channel = command_data.channel
                flag = command_data.args[1].lower()
                value = int(command_data.args[2]) == 1
            else:
                channel = command_data.args[0].lower()
                flag = command_data.args[2].lower()
                value = int(command_data.args[3]) == 1

            if not self._is_channel_admin(command_data, channel, command_data.issuer):
                return

            channel_data: ChannelData = bot_instance.channels[channel]

            if flag == 'all':
                for key, value in channel_data.chatter_settings['types'].items():
                    channel_data.chatter_settings['types'][key] = value

                channel_data.save_data()

                self.respond_to_user(command_data, f"All chatter flags updated.")
            else:
                if flag in channel_data.chatter_settings['types']:
                    channel_data.chatter_settings['types'][flag] = value
                    channel_data.save_data()
                    self.respond_to_user(command_data, f"Chatter flag updated.")
                else:
                    self.respond_to_user(command_data, f"I'm sorry, but I don't have a setting for {flag}")

        else:
            flags = ", ".join(ChannelData.chatter_settings_defaults['types'].keys())

            self.respond_to_user(command_data, "Usage: $chatterflag [<#channel>] list OR $chatterflag [<#channel>] set "
                                               "<type> <0/1>")
            self.respond_to_user(command_data, f"Valid Chatter Types: all, {flags}")

    def _commandflag_handler(self, command_data: CommandData) -> None:
        from timmy.core import bot_instance

        if 1 <= command_data.arg_count <= 2 and command_data.args[command_data.arg_count - 1].lower() == 'list':
            if command_data.arg_count == 1:
                channel = command_data.channel
            else:
                channel = command_data.args[0].lower()

            if not self._is_channel_admin(command_data, channel, command_data.issuer):
                return

            channel_data: ChannelData = bot_instance.channels[channel]

            self.respond_to_user(command_data, f"Sending status of command settings for {channel} via private message.")

            for key, value in channel_data.command_settings.items():
                bot_instance.connection.privmsg(command_data.issuer, f"{key}: {value}")

        elif 3 <= command_data.arg_count <= 4 and command_data.args[command_data.arg_count - 3].lower() == 'set':
            if command_data.arg_count == 3:
                channel = command_data.channel
                command = command_data.args[1].lower()
                value = int(command_data.args[2]) == 1
            else:
                channel = command_data.args[0].lower()
                command = command_data.args[2].lower()
                value = int(command_data.args[3]) == 1

            if not self._is_channel_admin(command_data, channel, command_data.issuer):
                return

            channel_data: ChannelData = bot_instance.channels[channel]

            if command == 'all':
                for key, value in channel_data.command_settings.items():
                    channel_data.command_settings[key] = value

                channel_data.save_data()

                self.respond_to_user(command_data, f"All command flags updated.")
            else:
                if command in channel_data.command_settings:
                    channel_data.command_settings[command] = value
                    channel_data.save_data()
                    self.respond_to_user(command_data, f"Command flag updated.")
                else:
                    self.respond_to_user(command_data, f"I'm sorry, but I don't have a setting for {command}")

        else:
            commands = ", ".join(ChannelData.command_defaults.keys())

            self.respond_to_user(command_data, "Usage: $commandflag [<#channel>] list OR $commandflag [<#channel>] set "
                                               "<type> <0/1>")
            self.respond_to_user(command_data, f"Valid Command Types: all, {commands}")
