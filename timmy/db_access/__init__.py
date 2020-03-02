from .connection_pool import ConnectionPool
from .settings import Settings
from .userdirectory import UserDirectory
from .word_war_db import WordWarDb
from .channel_db import ChannelDb

connection_pool = ConnectionPool()
settings = Settings()
word_war_db = WordWarDb()
channel_db = ChannelDb()
user_directory = UserDirectory()


def init_db_access():
    global settings
    settings.init()

    global word_war_db
    word_war_db.init()

    global channel_db
    channel_db.init()
