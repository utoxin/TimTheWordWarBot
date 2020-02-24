from timmy.core.bot import Bot
from timmy.core.war_ticker import WarTicker

bot_instance = Bot()
war_ticker_instance = WarTicker()


def init_war_ticker():
    global war_ticker_instance
    war_ticker_instance.load_wars()
