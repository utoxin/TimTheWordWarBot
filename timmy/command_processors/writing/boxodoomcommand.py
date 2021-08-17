from irc.client import Event, ServerConnection

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData


class BoxODoomCommand(BaseCommand):
    user_commands = {"boxodoom"}

    def process(self, connection: ServerConnection, event: Event, command_data: CommandData) -> None:
        