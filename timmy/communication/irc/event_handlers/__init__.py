from timmy.communication.irc.event_handlers.auth_handler import AuthHandler

auth_handler_instance = AuthHandler()


def init_event_handlers(auth_config: dict):
    global auth_handler_instance

    auth_handler_instance.init(auth_config)

    from timmy.communication.irc.irc import Irc

    Irc.bot.handled_callbacks["umode"].append(auth_handler_instance)
    Irc.bot.handled_callbacks["welcome"].append(auth_handler_instance)
