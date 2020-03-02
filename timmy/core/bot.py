import itertools

import irc.client_aio
import more_itertools
from irc.bot import ExponentialBackoff, ServerSpec
from irc.dict import IRCDict
from irc import client, modes
from timmy import db_access, event_handlers
from timmy.data.channel_data import ChannelData


class Bot(irc.client_aio.AioSimpleIRCClient):
    def __init__(self):
        super().__init__()

        self.handled_callbacks = {
            "action":       [],
            "disconnect":   [],
            "invite":       [],
            "join":         [],
            "kick":         [],
            "namreply":     [],
            "nick":         [],
            "part":         [],
            "privmsg":      [],
            "pubmsg":       [],
            "quit":         [],
            "umode":        [],
            "welcome":      [],
            "whoisaccount": [],
        }

        self.channels = IRCDict()
        self._nickname = "Timmy"
        self._realname = "Timmy"
        self.servers = None
        self.recon = None

    def setup(self, host, database, user, password):
        for i in self.handled_callbacks.keys():
            self.connection.add_global_handler(i, getattr(self, "_on_" + i), -20)

        db_access.init_db_access()

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

    def _on_action(self, connection, event):
        for obj in self.handled_callbacks["action"]:
            if obj.on_action(connection, event):
                break

    def _on_disconnect(self, connection, event):
        self.channels = IRCDict()
        self.recon.run(self)

        for obj in self.handled_callbacks["disconnect"]:
            if obj.on_disconnect(connection, event):
                break

    def _on_invite(self, connection, event):
        for obj in self.handled_callbacks["invite"]:
            if obj.on_invite(connection, event):
                break

    def _on_join(self, connection, event):
        ch = event.target
        nick = event.source.nick

        if nick == connection.get_nickname():
            self.channels[ch] = ChannelData(ch)
            self.channels[ch].join_channel()

        self.channels[ch].add_user(nick)

        for obj in self.handled_callbacks["join"]:
            if obj.on_join(connection, event):
                break

    def _on_kick(self, connection, event):
        nick = event.arguments[0]
        channel = event.target

        if nick == connection.get_nickname():
            self.channels[channel].leave_channel()
            del self.channels[channel]
        else:
            self.channels[channel].remove_user(nick)

        for obj in self.handled_callbacks["kick"]:
            if obj.on_kick(connection, event):
                break

    def _on_mode(self, connection, event):
        t = event.target
        if not irc.client.is_channel(t):
            # mode on self; disregard
            return
        ch = self.channels[t]

        channel_modes = irc.modes.parse_channel_modes(" ".join(event.arguments))
        for sign, mode, argument in channel_modes:
            f = {"+": ch.set_mode, "-": ch.clear_mode}[sign]
            f(mode, argument)

        for obj in self.handled_callbacks["mode"]:
            if obj.on_mode(connection, event):
                break

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
            if obj.on_namreply(connection, event):
                break

    def _on_nick(self, connection, event):
        before = event.source.nick
        after = event.target
        for ch in self.channels.values():
            if ch.has_user(before):
                ch.change_nick(before, after)

        for obj in self.handled_callbacks["nick"]:
            if obj.on_nick(connection, event):
                break

    def _on_part(self, connection, event):
        nick = event.source.nick
        channel = event.target

        if nick == connection.get_nickname():
            del self.channels[channel]
        else:
            self.channels[channel].remove_user(nick)

        for obj in self.handled_callbacks["part"]:
            if obj.on_part(connection, event):
                break

    def _on_privmsg(self, connection, event):
        for obj in self.handled_callbacks["privmsg"]:
            if obj.on_privmsg(connection, event):
                break

    def _on_pubmsg(self, connection, event):
        for obj in self.handled_callbacks["pubmsg"]:
            if obj.on_pubmsg(connection, event):
                break

    def _on_quit(self, connection, event):
        nick = event.source.nick
        for ch in self.channels.values():
            if ch.has_user(nick):
                ch.remove_user(nick)

        for obj in self.handled_callbacks["quit"]:
            if obj.on_quit(connection, event):
                break

    def _on_umode(self, connection, event):
        for obj in self.handled_callbacks["umode"]:
            if obj.on_umode(connection, event):
                break

    def _on_welcome(self, connection, event):
        for obj in self.handled_callbacks["welcome"]:
            if obj.on_welcome(connection, event):
                break

    def _on_whoisaccount(self, connection, event):
        for obj in self.handled_callbacks["whoisaccount"]:
            if obj.on_whoisaccount(connection, event):
                break
