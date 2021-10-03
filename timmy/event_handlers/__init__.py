from .auth_handler import AuthHandler
from .command_handler import CommandHandler
from .postauth_handler import PostAuthHandler
from .reaction_handler import ReactionHandler
from .server_handler import ServerHandler
from .whois_handler import WhoisHandler

server_handler_instance = ServerHandler()
auth_handler_instance = AuthHandler()
postauth_handler_instance = PostAuthHandler()
command_handler_instance = CommandHandler()
whois_handler_instance = WhoisHandler()
reaction_handler_instance = ReactionHandler()


def init_event_handlers():
    from timmy.core import bot_instance

    global auth_handler_instance
    global command_handler_instance
    global postauth_handler_instance
    global reaction_handler_instance
    global server_handler_instance
    global whois_handler_instance

    auth_handler_instance.init()
    command_handler_instance.init_command_processors()

    bot_instance.handled_callbacks["action"].append(whois_handler_instance)
    bot_instance.handled_callbacks['action'].append(reaction_handler_instance)

    bot_instance.handled_callbacks["invite"].append(server_handler_instance)

    bot_instance.handled_callbacks["join"].append(server_handler_instance)
    bot_instance.handled_callbacks["join"].append(whois_handler_instance)

    bot_instance.handled_callbacks["namreply"].append(whois_handler_instance)

    bot_instance.handled_callbacks["nick"].append(whois_handler_instance)

    bot_instance.handled_callbacks["pubmsg"].append(whois_handler_instance)
    bot_instance.handled_callbacks["pubmsg"].append(command_handler_instance)
    bot_instance.handled_callbacks['pubmsg'].append(reaction_handler_instance)

    bot_instance.handled_callbacks["privmsg"].append(whois_handler_instance)
    bot_instance.handled_callbacks["privmsg"].append(command_handler_instance)
    bot_instance.handled_callbacks['privmsg'].append(reaction_handler_instance)

    bot_instance.handled_callbacks["umode"].append(auth_handler_instance)
    bot_instance.handled_callbacks["umode"].append(postauth_handler_instance)

    bot_instance.handled_callbacks["welcome"].append(auth_handler_instance)

    bot_instance.handled_callbacks["whoisaccount"].append(whois_handler_instance)
