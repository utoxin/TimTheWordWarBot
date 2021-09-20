from irc.client import Event, ServerConnection

from timmy import core
from timmy.command_processors.base_command import BaseCommand
from timmy.data.channel_data import ChannelData
from timmy.data.command_data import CommandData


class RaptorStatsCommand(BaseCommand):
    user_commands = {'raptorstats'}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        if not command_data.in_pm:
            channel_data: ChannelData = core.bot_instance.channels[command_data.channel]

            if not channel_data.command_settings['velociraptor']:
                self.respond_to_user(connection, event, "I'm sorry, I don't do that here.")
                return

            self.respond_to_user(connection, event, f"There have been {channel_data.raptor_data['sightings']} "
                                                   f"velociraptor sightings in this channel, "
                                                   f"and {channel_data.raptor_data['active']} are still here.")

            self.respond_to_user(connection, event, f"{channel_data.raptor_data['dead']} velociraptors in this "
                                                    f"channel have been killed by other swarms.")

            self.respond_to_user(connection, event, f"Swarms from this channel have killed "
                                                    f"{channel_data.raptor_data['killed']} other velociraptors.")

            if channel_data.raptor_data['strength'] > 0:
                self.respond_to_user(connection, event, f"It looks like the training you've been doing has helped "
                                                        f"organize the local raptors! The area can now support "
                                                        f"{100 + channel_data.raptor_data['strength']} "
                                                        f"of them.")

        else:
            self.respond_to_user(connection, event, "Sorry, that doesn't make sense to do here.")

        return
