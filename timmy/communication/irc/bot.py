import itertools
import re
import uuid
from typing import Optional

import irc.client_aio
import more_itertools
from irc.bot import Channel, ExponentialBackoff, ServerSpec
from irc.client import Event, ServerConnection
from irc.dict import IRCDict
from pubsub import pub


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
        self.connection_tag = None

    def setup(
            self, connection_tag: uuid, nickname: str, realname: str, host: str, port: int,
            server_password: Optional[str], auth_config: dict
            ) -> None:
        self.connection_tag = connection_tag

        for i in self.handled_callbacks.keys():
            self.connection.add_global_handler(i, getattr(self, "_on_" + i), -20)

        self._nickname = nickname
        self._realname = realname

        server = ServerSpec(host, port, server_password)
        specs = map(ServerSpec.ensure, [server])

        self.servers = more_itertools.peekable(itertools.cycle(specs))
        self.recon = ExponentialBackoff()

        from timmy.communication.irc import event_handlers
        event_handlers.init_event_handlers(auth_config)

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

        pub.sendMessage(
            "action-received", message_data={
                    "connection_tag": self.connection_tag,
                    "sender":         event.source,
                    "channel":        event.target,
                    "message":        event.arguments[0]
                }
            )

    def _on_disconnect(self, connection: ServerConnection, event: Event) -> None:
        self.channels = IRCDict()
        self.recon.run(self)

        for obj in self.handled_callbacks["disconnect"]:
            if obj.on_disconnect(connection, event):
                break

    def _on_invite(self, connection: ServerConnection, event: Event) -> None:
        pub.sendMessage(
            "invite-received", message_data={
                    "connection_tag": self.connection_tag,
                    "channel":        event.target
                }
            )

    def _on_join(self, connection: ServerConnection, event: Event) -> None:
        ch = event.target
        nick = event.source.nick

        if event.source.nick == connection.get_nickname():
            if nick == connection.get_nickname():
                self.channels[ch] = Channel()

            pub.sendMessage(
                    "channel-joined-self", message_data={
                        "connection_tag": self.connection_tag,
                        "channel":        event.target,
                    }
            )
        else:
            pub.sendMessage(
                    "channel-joined-other", message_data={
                        "connection_tag": self.connection_tag,
                        "channel":        event.target,
                        "username":       event.source.nick
                    }
            )

        self.channels[ch].add_user(nick)

    def _on_kick(self, connection: ServerConnection, event: Event) -> None:
        if event.source.nick == connection.get_nickname():
            pub.sendMessage(
                    "channel-kicked-self", message_data={
                        "connection_tag": self.connection_tag,
                        "channel":        event.target,
                    }
            )
        else:
            pub.sendMessage(
                    "channel-kicked-other", message_data={
                        "connection_tag": self.connection_tag,
                        "channel":        event.target,
                        "username":       event.source.nick
                    }
            )

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
        pub.sendMessage(
                "nick-change", message_data={
                    "connection_tag": self.connection_tag,
                    "before":         event.source.nick,
                    "after":          event.target
                }
        )

    def _on_part(self, connection: ServerConnection, event: Event) -> None:
        if event.source.nick == connection.get_nickname():
            pub.sendMessage(
                    "channel-left-self", message_data={
                        "connection_tag": self.connection_tag,
                        "channel":        event.target,
                    }
            )
        else:
            pub.sendMessage(
                    "channel-left-other", message_data={
                        "connection_tag": self.connection_tag,
                        "channel":        event.target,
                        "username":       event.source.nick
                    }
            )

    def _on_privmsg(self, connection: ServerConnection, event: Event) -> None:
        event = self._cleanup_color(event)

        pub.sendMessage(
            "pm-received", message_data={
                    "connection_tag": self.connection_tag,
                    "sender":         event.source,
                    "message":        event.arguments[0]
                }
            )

    def _on_pubmsg(self, connection: ServerConnection, event: Event) -> None:
        event = self._cleanup_color(event)

        pub.sendMessage(
            "message-received", message_data={
                    "connection_tag": self.connection_tag,
                    "sender":         event.source,
                    "channel":        event.target,
                    "message":        event.arguments[0]
                }
            )

    def _on_quit(self, connection: ServerConnection, event: Event) -> None:
        if event.source.nick == connection.get_nickname():
            pub.sendMessage(
                    "quit-self", message_data={
                        "connection_tag": self.connection_tag,
                    }
            )
        else:
            pub.sendMessage(
                    "quit-other", message_data={
                        "connection_tag": self.connection_tag,
                        "channel":        event.target,
                        "username":       event.source.nick
                    }
            )

    def _on_umode(self, connection: ServerConnection, event: Event) -> None:
        for obj in self.handled_callbacks["umode"]:
            if obj.on_umode(connection, event):
                break

    def _on_welcome(self, connection: ServerConnection, event: Event) -> None:
        pub.sendMessage(
                "server-welcome", message_data={
                    "connection_tag": self.connection_tag
                }
        )

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
