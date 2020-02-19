from .auth_handler import AuthHandler
from .postauth_handler import PostAuthHandler
from .server_handler import ServerHandler
from .user_command_listener import UserCommandListener


def init_event_handlers():
    from timmy.core import bot_instance

    server_handler_instance = ServerHandler()
    bot_instance.handled_callbacks["invite"].append(server_handler_instance)
    bot_instance.handled_callbacks["join"].append(server_handler_instance)

    auth_handler_instance = AuthHandler()
    bot_instance.handled_callbacks["welcome"].append(auth_handler_instance)

    postauth_handler_instance = PostAuthHandler()
    bot_instance.handled_callbacks["umode"].append(postauth_handler_instance)

    user_command_handler_instance = UserCommandListener()
    bot_instance.handled_callbacks["pubmsg"].append(user_command_handler_instance)
