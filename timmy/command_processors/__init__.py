from timmy import event_handlers
from .amusement.attackcommand import AttackCommand
from .amusement.catchcommand import CatchCommand
from .amusement.commandmentcommand import CommandmentCommand
from .amusement.dance import DanceCommand
from .amusement.defenstrate import DefenestrateCommand
from .amusement.dicecommand import DiceCommand
from .amusement.eightball import EightballCommand
from .amusement.expound import ExpoundCommand
from .amusement.foof import FoofCommand
from .amusement.fridge import FridgeCommand
from .amusement.getcommands import GetCommands
from .amusement.herdcommand import HerdCommand
from .amusement.lickcommand import LickCommand
from .amusement.raptoprstats import RaptorStatsCommand
from .amusement.raptorcommands import RaptorCommands
from .amusement.summon import SummonCommand
from .amusement.sing import SingCommand
from .amusement.search import SearchCommand
from .amusement.woot import WootCommand
from .utility.admincommands import AdminCommands
from .utility.channelcommands import ChannelCommands
from .utility.channelgroupcommands import ChannelGroupCommands
from .utility.ignorecommands import IgnoreCommands
from .utility.interactioncontrols import InteractionControls
from .utility.pickonecommand import PickOneCommand
from .utility.pingcommand import PingCommand
from .utility.timercommand import TimerCommand
from .writing.boxodoomcommand import BoxODoomCommand
from .writing.challenge import ChallengeCommands
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

    eightball = EightballCommand()
    eightball.register_commands(event_handlers.command_handler_instance)

    dance = DanceCommand()
    dance.register_commands(event_handlers.command_handler_instance)

    foof = FoofCommand()
    foof.register_commands(event_handlers.command_handler_instance)

    fridge = FridgeCommand()
    fridge.register_commands(event_handlers.command_handler_instance)

    defenestrate = DefenestrateCommand()
    defenestrate.register_commands(event_handlers.command_handler_instance)

    woot = WootCommand()
    woot.register_commands(event_handlers.command_handler_instance)

    getcommands = GetCommands()
    getcommands.register_commands(event_handlers.command_handler_instance)

    challengecommand = ChallengeCommands()
    challengecommand.register_commands(event_handlers.command_handler_instance)

    summoncommand = SummonCommand()
    summoncommand.register_commands(event_handlers.command_handler_instance)

    sing = SingCommand()
    sing.register_commands(event_handlers.command_handler_instance)

    search = SearchCommand()
    search.register_commands(event_handlers.command_handler_instance)

    channel_groups = ChannelGroupCommands()
    channel_groups.register_commands(event_handlers.command_handler_instance)

    ignore_commands = IgnoreCommands()
    ignore_commands.register_commands(event_handlers.command_handler_instance)

    raptorstats = RaptorStatsCommand()
    raptorstats.register_commands(event_handlers.command_handler_instance)

    expound = ExpoundCommand()
    expound.register_commands(event_handlers.command_handler_instance)
