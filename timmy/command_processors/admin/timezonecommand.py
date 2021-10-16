from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from pytz import common_timezones


class TimezoneCommand(BaseCommand):
    admin_commands = ['timezone']

    help_topics = [('admin', 'core admin commands', '$timezone <timezone>', 'Update the current channel\'s timezone')]

    def process(self, command_data: CommandData) -> None:
        if command_data.arg_count == 0:
            self._usage(command_data)
            return

        target = command_data.channel
        timezone = "_".join(command_data.args)

        if not self._is_channel_admin(command_data, target, command_data.issuer):
            return

        if timezone in common_timezones:
            from timmy.core import bot_instance
            bot_instance.channels[target].set_timezone(timezone)
            self.respond_to_user(command_data, "Timezone updated for channel.")
        else:
            self.respond_to_user(command_data, "Unknown timezone. Please check the provided link, and supply a TZ "
                                               "Database Name.")
            self._usage(command_data)

    def _usage(self, command_data: CommandData) -> None:
        self.respond_to_user(command_data, "Usage: $timezone <timezone>")
        self.respond_to_user(command_data, "Example: $timezone America/Denver")
        self.respond_to_user(command_data, "You can find your timezone in the list on this page: "
                             "https://en.wikipedia.org/wiki/List_of_tz_database_time_zones and provide the TZ "
                             "Database Name.")