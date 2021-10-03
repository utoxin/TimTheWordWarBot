from irc.client import Event

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData


class RaptorStatsCommand(BaseCommand):
    user_commands = {'raptorstats'}

    allowed_in_pm = False
    interaction_checks = False
    command_flag_map = {
        'raptorstats': 'velociraptor'
    }

    def process(self, event: Event, command_data: CommandData) -> None:
        if self._execution_checks(event, command_data):
            channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

            self.respond_to_user(event, f"There have been {channel_data.raptor_data['sightings']} velociraptor "
                                        f"sightings in this channel, and {channel_data.raptor_data['active']} are still"
                                        f" here.")

            self.respond_to_user(event, f"{channel_data.raptor_data['dead']} velociraptors in this channel have been "
                                        f"killed by other swarms."
                                 )

            self.respond_to_user(event, f"Swarms from this channel have killed {channel_data.raptor_data['killed']} "
                                        f"other velociraptors."
                                 )

            if channel_data.raptor_data['strength'] > 0:
                self.respond_to_user(event, f"It looks like the training you've been doing has helped organize the "
                                            f"local raptors! The area can now support "
                                            f"{100 + channel_data.raptor_data['strength']} of them."
                                     )
