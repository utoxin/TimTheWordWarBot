from .auth_handler import AuthHandler
from .command_handler import CommandHandler
from .postauth_handler import PostAuthHandler
from .server_handler import ServerHandler

server_handler_instance = ServerHandler()
auth_handler_instance = AuthHandler()
postauth_handler_instance = PostAuthHandler()
command_handler_instance = CommandHandler()


def init_event_handlers():
    from timmy.core import bot_instance

    # Server Handler
    global server_handler_instance
    bot_instance.handled_callbacks["invite"].append(server_handler_instance)
    bot_instance.handled_callbacks["join"].append(server_handler_instance)

    # Auth Handler
    global auth_handler_instance
    auth_handler_instance.init()
    bot_instance.handled_callbacks["welcome"].append(auth_handler_instance)

    # Post Auth Handler
    global postauth_handler_instance
    bot_instance.handled_callbacks["umode"].append(postauth_handler_instance)

    # Command Handler
    global command_handler_instance
    command_handler_instance.init_command_processors()
    bot_instance.handled_callbacks["pubmsg"].append(command_handler_instance)
    bot_instance.handled_callbacks["privmsg"].append(command_handler_instance)
