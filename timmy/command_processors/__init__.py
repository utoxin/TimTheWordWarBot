from .admin.timezonecommand import TimezoneCommand
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
from .owner.loglevel import LogLevelCommand
from .owner.shutdown import ShutdownCommand
from .utility.admincommands import AdminCommands
from .utility.badwords import BadWordCommands
from .utility.channelcommands import ChannelCommands
from .utility.channelgroupcommands import ChannelGroupCommands
from .utility.creditscommand import CreditsCommand
from .utility.helpcommands import HelpCommands
from .utility.ignorecommands import IgnoreCommands
from .utility.interactioncontrols import InteractionControls
from .utility.pickonecommand import PickOneCommand
from .utility.pingcommand import PingCommand
from .utility.timercommand import TimerCommand
from .writing.boxodoomcommand import BoxODoomCommand
from .writing.chainstory import ChainStoryCommands
from .writing.challenge import ChallengeCommands
from .writing.warcommands import WarCommands

help_commands = HelpCommands()
interaction_controls = InteractionControls()


def register_processors():
    help_commands.late_init()
    interaction_controls.late_init()

    # Amusement Commands
    AttackCommand()
    CatchCommand()
    CommandmentCommand()
    DanceCommand()
    DefenestrateCommand()
    DiceCommand()
    EightballCommand()
    ExpoundCommand()
    FoofCommand()
    FridgeCommand()
    GetCommands()
    HerdCommand()
    LickCommand()
    RaptorCommands()
    RaptorStatsCommand()
    SingCommand()
    SearchCommand()
    SummonCommand()
    WootCommand()

    # Admin Commands
    AdminCommands()
    BadWordCommands()
    LogLevelCommand()
    ShutdownCommand()
    TimezoneCommand()

    # Utility Commands
    ChannelGroupCommands()
    ChannelCommands()
    CreditsCommand()
    IgnoreCommands()
    PickOneCommand()
    PingCommand()
    TimerCommand()

    # Writing Commands
    BoxODoomCommand()
    ChainStoryCommands()
    ChallengeCommands()
    WarCommands()
