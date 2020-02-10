import itertools

import irc.client_aio
import more_itertools
from irc.bot import ExponentialBackoff, ServerSpec, Channel
from irc.dict import IRCDict
from irc import client, modes
from timmy import db_access, event_handlers


class Bot(irc.client_aio.AioSimpleIRCClient):
    def __init__(self):
        super().__init__()

        self.handled_callbacks = {
            "action": [],
            "disconnect": [],
            "invite": [],
            "join": [],
            "kick": [],
            "namreply": [],
            "nick": [],
            "part": [],
            "privmsg": [],
            "pubmsg": [],
            "quit": [],
            "welcome": [],
        }

        self.channels = IRCDict()
        self._nickname = "Timmy"
        self._realname = "Timmy"
        self.servers = None
        self.recon = None

    def setup(self, host, database, user, password):
        for i in self.handled_callbacks.keys():
            self.connection.add_global_handler(i, getattr(self, "_on_" + i), -20)

        pool = db_access.connection_pool
        pool.setup(host, database, user, password)

        settings = db_access.settings

        self._nickname = settings.get_setting("nickname")
        self._realname = settings.get_setting("realname")

        server = ServerSpec(settings.get_setting("host"))
        specs = map(ServerSpec.ensure, [server])

        self.servers = more_itertools.peekable(itertools.cycle(specs))
        self.recon = ExponentialBackoff()

        event_handlers.init_event_handlers()

    def start(self):
        self._connect()
        self.reactor.process_forever()

    def _connect(self):
        server = self.servers.peek()
        try:
            self.connect(
                server.host,
                server.port,
                self._nickname,
                server.password,
                ircname=self._realname
            )
        except irc.client_aio.ServerNotConnectedError:
            pass

    def _on_disconnect(self, connection, event):
        self.channels = IRCDict()
        self.recon.run(self)

        for obj in self.handled_callbacks["disconnect"]:
            obj.on_disconnect(connection, event)

    def _on_join(self, connection, event):
        ch = event.target
        nick = event.source.nick
        if nick == connection.get_nickname():
            self.channels[ch] = Channel()
        self.channels[ch].add_user(nick)

        for obj in self.handled_callbacks["join"]:
            obj.on_join(connection, event)

    def _on_kick(self, connection, event):
        nick = event.arguments[0]
        channel = event.target

        if nick == connection.get_nickname():
            del self.channels[channel]
        else:
            self.channels[channel].remove_user(nick)

        for obj in self.handled_callbacks["kick"]:
            obj.on_kick(connection, event)

    def _on_mode(self, connection, event):
        t = event.target
        if not irc.client.is_channel(t):
            # mode on self; disregard
            return
        ch = self.channels[t]

        modes = irc.modes.parse_channel_modes(" ".join(event.arguments))
        for sign, mode, argument in modes:
            f = {"+": ch.set_mode, "-": ch.clear_mode}[sign]
            f(mode, argument)

        for obj in self.handled_callbacks["mode"]:
            obj.on_mode(connection, event)

    def _on_namreply(self, connection, event):
        """
        event.arguments[0] == "@" for secret channels,
                          "*" for private channels,
                          "=" for others (public channels)
        event.arguments[1] == channel
        event.arguments[2] == nick list
        """

        ch_type, channel, nick_list = event.arguments

        if channel == '*':
            # User is not in any visible channel
            # http://tools.ietf.org/html/rfc2812#section-3.2.5
            return

        for nick in nick_list.split():
            nick_modes = []

            if nick[0] in self.connection.features.prefix:
                nick_modes.append(self.connection.features.prefix[nick[0]])
                nick = nick[1:]

            for mode in nick_modes:
                self.channels[channel].set_mode(mode, nick)

            self.channels[channel].add_user(nick)

        for obj in self.handled_callbacks["namreply"]:
            obj.on_namreply(connection, event)

    def _on_nick(self, connection, event):
        before = event.source.nick
        after = event.target
        for ch in self.channels.values():
            if ch.has_user(before):
                ch.change_nick(before, after)

        for obj in self.handled_callbacks["nick"]:
            obj.on_nick(connection, event)

    def _on_part(self, connection, event):
        nick = event.source.nick
        channel = event.target

        if nick == connection.get_nickname():
            del self.channels[channel]
        else:
            self.channels[channel].remove_user(nick)

        for obj in self.handled_callbacks["part"]:
            obj.on_part(connection, event)

    def _on_quit(self, connection, event):
        nick = event.source.nick
        for ch in self.channels.values():
            if ch.has_user(nick):
                ch.remove_user(nick)

        for obj in self.handled_callbacks["quit"]:
            obj.on_quit(connection, event)

    def _on_privmsg(self, connection, event):
        for obj in self.handled_callbacks["privmsg"]:
            obj.on_privmsg(connection, event)

    def _on_welcome(self, connection, event):
        for obj in self.handled_callbacks["welcome"]:
            obj.on_welcome(connection, event)

    def _on_pubmsg(self, connection, event):
        for obj in self.handled_callbacks["pubmsg"]:
            obj.on_pubmsg(connection, event)

    def _on_action(self, connection, event):
        for obj in self.handled_callbacks["action"]:
            obj.on_action(connection, event)

    def _on_invite(self, connection, event):
        for obj in self.handled_callbacks["invite"]:
            obj.on_invite(connection, event)
