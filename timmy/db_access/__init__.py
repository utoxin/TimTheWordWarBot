from .connection_pool import ConnectionPool
from .settings import Settings
from .word_war_db import WordWarDb
from .channel_db import ChannelDb

connection_pool = ConnectionPool()
settings = Settings()
word_war_db = WordWarDb()
channel_db = ChannelDb()
