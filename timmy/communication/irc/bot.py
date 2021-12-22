import itertools
import re
from typing import Optional

import irc.client_aio
import more_itertools
from irc.bot import ExponentialBackoff, ServerSpec
from irc.client import Event, ServerConnection
from irc.dict import IRCDict


class Bot(irc.client_aio.AioSimpleIRCClient):
    def __init__(self):
        super().__init__()

        self.handled_callbacks = {
            "action":       [],
            "disconnect":   [],
            "invite":       [],
            "join":         [],
            "kick":         [],
            "mode":         [],
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

    def setup(self, nickname: str, realname: str, host: str, port: int, server_password: Optional[str]) -> None:
        for i in self.handled_callbacks.keys():
            self.connection.add_global_handler(i, getattr(self, "_on_" + i), -20)

        self._nickname = nickname
        self._realname = realname

        server = ServerSpec(host, port, server_password)
        specs = map(ServerSpec.ensure, [server])

        self.servers = more_itertools.peekable(itertools.cycle(specs))
        self.recon = ExponentialBackoff()

    def start(self) -> None:
        self._connect()
        self.reactor.process_forever()

    def _connect(self) -> None:
        server = self.servers.peek()
        try:
            self.connect(
                    server.host,
                    server.port,
                    self._nickname,
                    server.password,
                    ircname = self._realname
            )
        except irc.client_aio.ServerNotConnectedError:
            pass

    def _on_action(self, connection: ServerConnection, event: Event) -> None:
        event = self._cleanup_color(event)
        for obj in self.handled_callbacks["action"]:
            if obj.on_action(connection, event):
                break

    def _on_disconnect(self, connection: ServerConnection, event: Event) -> None:
        self.channels = IRCDict()
        self.recon.run(self)

        for obj in self.handled_callbacks["disconnect"]:
            if obj.on_disconnect(connection, event):
                break

    def _on_invite(self, connection: ServerConnection, event: Event) -> None:
        for obj in self.handled_callbacks["invite"]:
            if obj.on_invite(connection, event):
                break

    def _on_join(self, connection: ServerConnection, event: Event) -> None:
        ch = event.target
        nick = event.source.nick

        if nick == connection.get_nickname():
            self.channels[ch] = ChannelData(ch)
            self.channels[ch].join_channel()

        self.channels[ch].add_user(nick)

        for obj in self.handled_callbacks["join"]:
            if obj.on_join(connection, event):
                break

    def _on_kick(self, connection: ServerConnection, event: Event) -> None:
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

    def _on_mode(self, connection: ServerConnection, event: Event) -> None:
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

    def _on_namreply(self, connection: ServerConnection, event: Event) -> None:
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

    def _on_nick(self, connection: ServerConnection, event: Event) -> None:
        before = event.source.nick
        after = event.target
        for ch in self.channels.values():
            if ch.has_user(before):
                ch.change_nick(before, after)

        for obj in self.handled_callbacks["nick"]:
            if obj.on_nick(connection, event):
                break

    def _on_part(self, connection: ServerConnection, event: Event) -> None:
        nick = event.source.nick
        channel = event.target

        if nick == connection.get_nickname():
            del self.channels[channel]
        else:
            self.channels[channel].remove_user(nick)

        for obj in self.handled_callbacks["part"]:
            if obj.on_part(connection, event):
                break

    def _on_privmsg(self, connection: ServerConnection, event: Event) -> None:
        event = self._cleanup_color(event)
        for obj in self.handled_callbacks["privmsg"]:
            if obj.on_privmsg(connection, event):
                break

    def _on_pubmsg(self, connection: ServerConnection, event: Event) -> None:
        event = self._cleanup_color(event)
        for obj in self.handled_callbacks["pubmsg"]:
            if obj.on_pubmsg(connection, event):
                break

    def _on_quit(self, connection: ServerConnection, event: Event) -> None:
        nick = event.source.nick
        for ch in self.channels.values():
            if ch.has_user(nick):
                ch.remove_user(nick)

        for obj in self.handled_callbacks["quit"]:
            if obj.on_quit(connection, event):
                break

    def _on_umode(self, connection: ServerConnection, event: Event) -> None:
        for obj in self.handled_callbacks["umode"]:
            if obj.on_umode(connection, event):
                break

    def _on_welcome(self, connection: ServerConnection, event: Event) -> None:
        for obj in self.handled_callbacks["welcome"]:
            if obj.on_welcome(connection, event):
                break

    def _on_whoisaccount(self, connection: ServerConnection, event: Event) -> None:
        for obj in self.handled_callbacks["whoisaccount"]:
            if obj.on_whoisaccount(connection, event):
                break

    @staticmethod
    def _cleanup_color(event: Event) -> Event:
        event.arguments[0] = re.sub(r'[\x02\x1F\x0F\x16]|\x03(\d\d?(,\d\d?)?)?', '', event.arguments[0])

        return event
