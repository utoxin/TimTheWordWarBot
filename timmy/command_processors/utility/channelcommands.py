import time

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class ChannelCommands(BaseCommand):
    admin_commands = {'automuzzlewars', 'muzzle', 'chatterlevel', 'chatterflag', 'commandflag',
                      'twitterrelay', 'twitterbucket', 'part', 'unmuzzle'}

    def process(self, connection, event, command_data: CommandData):
        from timmy.core import user_perms
        if not user_perms.is_admin(command_data.issuer, command_data.channel):
            self.respond_to_user(connection, event, "I'm sorry, only admins are allowed to do that.")
            return

        command_handler = getattr(self, '_' + command_data.command + '_handler')
        command_handler(connection, event, command_data)

    def _validate_channel(self, connection, event, candidate_channel, user):
        from timmy.core import user_perms, bot_instance

        if candidate_channel not in bot_instance.channels:
            self.respond_to_user(connection, event, "I don't know about '{}'. Sorry.".format(candidate_channel))
            return False

        if not user_perms.is_admin(user, candidate_channel):
            self.respond_to_user(connection, event, "I'm sorry, you aren't an admin for that channel.")
            return False

        return True

    def _unmuzzle_handler(self, connection, event, command_data: CommandData):
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

    def _muzzle_handler(self, connection, event, command_data: CommandData):
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

            target = command_data.args[0]
            duration = float(command_data.args[1])

        if duration < 0:
            expiration = None
        else:
            expiration = time.time() + duration * 60

        bot_instance.channels[target].set_muzzle_flag(True, expiration)

        self.respond_to_user(connection, event, "Channel muzzled for specified time.")
        return

    def _automuzzlewars_handler(self, connection, event, command_data: CommandData):
        from timmy.core import bot_instance

        if command_data.arg_count == 0:
            self.respond_to_user(connection, event, "Usage: $automuzzlewars [#channel] <0/1>")
            self.respond_to_user(connection, event, "Usage: Whether Timmy should auto-muzzle during word wars.")
            self.respond_to_user(connection, event, "Usage: 0 disables this feature, 1 enables it.")
            self.respond_to_user(connection, event, "Example: $automuzzlewars 1  --  Enables automuzzle for this "
                                                    "channel")
            return

        if command_data.arg_count == 1:
            target = command_data.channel
            flag = int(command_data.args[0]) == 1
        else:
            if not self._validate_channel(connection, event, command_data.args[0], command_data.issuer):
                return

            target = command_data.args[0]
            flag = int(command_data.args[1]) == 1

        bot_instance.channels[target].set_muzzle_flag(flag)

        self.respond_to_user(connection, event, "Channel auto muzzle flag updated for {}.".format(target))
        return

    def _chatterlevel_handler(self, connection, event, command_data: CommandData):
        from timmy.core import bot_instance

        def output_usage():
            self.respond_to_user(connection, event, "Usage: $chatterlevel <#channel> list")
            self.respond_to_user(connection, event, "Usage: $chatterlevel <#channel> set <%/Msg> <Name Multiplier> "
                                                    "<%/Min>")

        if command_data.arg_count == 1 or command_data.arg_count == 2:
            if command_data.arg_count == 1:
                channel = command_data.channel
                command = command_data.args[0]
            else:
                channel = command_data.args[0]
                command = command_data.args[1]

                if not self._validate_channel(connection, event, channel, command_data.issuer):
                    return

            if command == 'list':
                from timmy.data.channel_data import ChannelData
                channel_data: ChannelData = bot_instance.channels[channel]

                reactive = channel_data.chatter_settings['reactive_level']
                random = channel_data.chatter_settings['random_level']
                name = channel_data.chatter_settings['name_multiplier']

                self.respond_to_user(connection, event, f"Reactive Chatter Level: {reactive:.3f}%/Msg - "
                                                        f"Name Multiplier: {name:.3f}")
                self.respond_to_user(connection, event, f"Random Chatter Level: {random:.3f}%/Min")
            else:
                output_usage()
                return

        elif command_data.arg_count == 4 or command_data.arg_count == 5:
            if command_data.arg_count == 4:
                channel = command_data.channel
                command = command_data.args[0]
                reactive = float(command_data.args[1])
                name = float(command_data.args[2])
                random = float(command_data.args[3])
            else:
                channel = command_data.args[0]
                command = command_data.args[1]
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

                channel_data.set_chatterflags(reactive, name, random)

                self.respond_to_user(connection, event, f"Reactive Chatter Level: {reactive:.3f}%/Msg - "
                                                        f"Name Multiplier: {name:.3f}")
                self.respond_to_user(connection, event, f"Random Chatter Level: {random:.3f}%/Min")
            else:
                output_usage()
                return

        else:
            output_usage()
            return

        return
