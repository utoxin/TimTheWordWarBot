from timmy.core.bot import Bot
from timmy.core.idleticker import IdleTicker
from timmy.core.raptorticker import RaptorTicker
from timmy.core.userperms import UserPerms
from timmy.core.warticker import WarTicker

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
