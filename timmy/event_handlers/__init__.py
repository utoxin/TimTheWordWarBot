from .auth_handler import AuthHandler
from .server_handler import ServerHandler


def init_event_handlers():
    from timmy.core import bot_instance

    server_handler_instance = ServerHandler()
    bot_instance.handled_callbacks["invite"].append(server_handler_instance)
    bot_instance.handled_callbacks["join"].append(server_handler_instance)

    auth_handler_instance = AuthHandler()
    bot_instance.handled_callbacks["welcome"].append(auth_handler_instance)
