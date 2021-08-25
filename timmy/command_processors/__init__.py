from .amusement.attackcommand import AttackCommand
from .amusement.catchcommand import CatchCommand
from .amusement.commandmentcommand import CommandmentCommand
from .amusement.dicecommand import DiceCommand
from .amusement.herdcommand import HerdCommand
from .amusement.lickcommand import LickCommand
from .amusement.raptorcommands import RaptorCommands
from .utility.admincommands import AdminCommands
from .utility.channelcommands import ChannelCommands
from .utility.interactioncontrols import InteractionControls
from .utility.pickonecommand import PickOneCommand
from .utility.pingcommand import PingCommand
from .utility.timercommand import TimerCommand
from timmy import event_handlers
from .writing.boxodoomcommand import BoxODoomCommand
from .writing.warcommands import WarCommands

interaction_controls = InteractionControls()


def register_processors():
    timer = TimerCommand()
    timer.register_commands(event_handlers.command_handler_instance)

    wars = WarCommands()
    wars.register_commands(event_handlers.command_handler_instance)

    raptors = RaptorCommands()
    raptors.register_commands(event_handlers.command_handler_instance)

    admins = AdminCommands()
    admins.register_commands(event_handlers.command_handler_instance)

    attack = AttackCommand()
    attack.register_commands(event_handlers.command_handler_instance)

    channels = ChannelCommands()
    channels.register_commands(event_handlers.command_handler_instance)

    interaction_controls.register_commands(event_handlers.command_handler_instance)

    dice = DiceCommand()
    dice.register_commands(event_handlers.command_handler_instance)

    boxodoom = BoxODoomCommand()
    boxodoom.register_commands(event_handlers.command_handler_instance)

    catch = CatchCommand()
    catch.register_commands(event_handlers.command_handler_instance)

    ping = PingCommand()
    ping.register_commands(event_handlers.command_handler_instance)

    pickone = PickOneCommand()
    pickone.register_commands(event_handlers.command_handler_instance)

    commandment = CommandmentCommand()
    commandment.register_commands(event_handlers.command_handler_instance)

    herd = HerdCommand()
    herd.register_commands(event_handlers.command_handler_instance)

    lick = LickCommand()
    lick.register_commands(event_handlers.command_handler_instance)

