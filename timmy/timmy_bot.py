import itertools
import logging
import sys

import irc.client_aio
import more_itertools
from irc.bot import ExponentialBackoff, ServerSpec, Channel
from irc.dict import IRCDict
from irc import client, modes
from timmy import db


class TimmyBot(irc.client_aio.AioSimpleIRCClient):
    handled_callbacks = [
        "disconnect",
        "join",
        "kick",
        "namreply",
        "nick",
        "part",
        "quit",
        "privmsg",
        "welcome"
    ]

    def __init__(self, host, database, user, password):
        super(TimmyBot, self).__init__()

        for i in self.handled_callbacks:
            self.connection.add_global_handler(i, getattr(self, "_on_" + i), -20)

        pool = db.ConnectionPool.instance()
        pool.setup(host, database, user, password)

        settings = db.Settings()

        self._nickname = settings.get_setting("nickname")
        self._realname = settings.get_setting("realname")

        self.channels = IRCDict()

        server = ServerSpec(settings.get_setting("host"))
        specs = map(ServerSpec.ensure, [server])

        self.servers = more_itertools.peekable(itertools.cycle(specs))
        self.recon = ExponentialBackoff()

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

    def _on_join(self, connection, event):
        ch = event.target
        nick = event.source.nick
        if nick == connection.get_nickname():
            self.channels[ch] = Channel()
        self.channels[ch].add_user(nick)

    def _on_kick(self, connection, event):
        nick = event.arguments[0]
        channel = event.target

        if nick == connection.get_nickname():
            del self.channels[channel]
        else:
            self.channels[channel].remove_user(nick)

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

    def _on_nick(self, connection, event):
        before = event.source.nick
        after = event.target
        for ch in self.channels.values():
            if ch.has_user(before):
                ch.change_nick(before, after)

    def _on_part(self, connection, event):
        nick = event.source.nick
        channel = event.target

        if nick == connection.get_nickname():
            del self.channels[channel]
        else:
            self.channels[channel].remove_user(nick)

    def _on_quit(self, connection, event):
        nick = event.source.nick
        for ch in self.channels.values():
            if ch.has_user(nick):
                ch.remove_user(nick)

    def _on_privmsg(self, connection, event):
        log.log(logging.DEBUG, "Handling privmsg")
        if event.arguments[0] == "foobar":
            connection.join("#feartimmy")

    def _on_welcome(self, connection, event):
        connection.join("#feartimmy")


if __name__ == "__main__":
    import configparser

    # Set up logging
    log = logging.getLogger("irc.client")
    log.setLevel(logging.DEBUG)

    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(logging.DEBUG)
    formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    handler.setFormatter(formatter)

    log.addHandler(handler)

    # Load config
    config = configparser.ConfigParser()
    config.read('botconfig.ini')

    # No config data found. Save out a template file, and exit.
    if len(config.sections()) == 0:
        print("No config data found. Template file created as botconfig.ini. Edit file and restart.")

        config.add_section("DB")
        config.set("DB", "host", "localhost")
        config.set("DB", "database", "timmy")
        config.set("DB", "user", "timmy")
        config.set("DB", "password", "password")

        with open('botconfig.ini', 'wb') as configfile:
            config.write(configfile)

        exit(1)

    # Load up our bot, and fire things up
    bot = TimmyBot(
        config.get("DB", "host"),
        config.get("DB", "database"),
        config.get("DB", "user"),
        config.get("DB", "password")
    )
    bot.start()
