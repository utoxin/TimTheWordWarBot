from .boxodoom_db import BoxODoomDb
from .chainstory_db import ChainStoryDb
from .channel_db import ChannelDb
from .connection_pool import ConnectionPool
from .settings import Settings
from .userdirectory import UserDirectory
from .word_war_db import WordWarDb

connection_pool = ConnectionPool()
settings = Settings()
word_war_db = WordWarDb()
channel_db = ChannelDb()
user_directory = UserDirectory()
boxodoom_db = BoxODoomDb()
chainstory_db = ChainStoryDb()


def init_db_access():
    global settings
    settings.init()

    global word_war_db
    word_war_db.init()

    global channel_db
    channel_db.init()

    global boxodoom_db
    boxodoom_db.init()

    global chainstory_db
    chainstory_db.init()