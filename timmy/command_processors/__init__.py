from .amusement.raptorcommands import RaptorCommands
from .utility.timercommand import TimerCommand
from timmy.event_handlers import command_handler_instance
from .writing.warcommands import WarCommands


def register_processors():
    timer = TimerCommand()
    timer.register_commands(command_handler_instance)

    wars = WarCommands()
    wars.register_commands(command_handler_instance)

    raptors = RaptorCommands()
    raptors.register_commands(command_handler_instance)
