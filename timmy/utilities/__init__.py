from timmy.utilities.irclogger import IrcLogger
from timmy.utilities.markovchains import MarkovChains
from timmy.utilities.markovprocessor import MarkovProcessor
from timmy.utilities.textgenerator import TextGenerator

text_generator = TextGenerator()
irc_logger = IrcLogger()
markov_processor = MarkovProcessor()
markov_generator = MarkovChains()


def init_utility_tickers():
    global markov_processor
    markov_processor.init()
