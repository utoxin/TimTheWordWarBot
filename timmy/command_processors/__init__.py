from .amusement.raptorcommands import RaptorCommands
from .utility.interactioncontrols import InteractionControls
from .utility.timercommand import TimerCommand
from timmy import event_handlers
from .writing.warcommands import WarCommands

interaction_controls = InteractionControls()


def register_processors():
    timer = TimerCommand()
    timer.register_commands(event_handlers.command_handler_instance)

    wars = WarCommands()
    wars.register_commands(event_handlers.command_handler_instance)

    raptors = RaptorCommands()
    raptors.register_commands(event_handlers.command_handler_instance)

    interaction_controls.register_commands(event_handlers.command_handler_instance)
