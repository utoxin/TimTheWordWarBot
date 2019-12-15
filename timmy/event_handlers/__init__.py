from .server_handler import ServerHandler


def init_event_handlers():
    from timmy.timmy_bot import TimmyBot

    timmy = TimmyBot.instance()
    timmy.handled_callbacks["invite"] = ServerHandler()
