from .bot import Bot
from .idleticker import IdleTicker
from .raptorticker import RaptorTicker
from .userperms import UserPerms
from .warticker import WarTicker

bot_instance = Bot()
war_ticker = WarTicker()
raptor_ticker = RaptorTicker()
user_perms = UserPerms()
idle_ticker = IdleTicker()


def init_core_tickers():
    global war_ticker
    war_ticker.load_wars()

    global idle_ticker
    idle_ticker.init()
