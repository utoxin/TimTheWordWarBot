import itertools

import irc.client_aio
import more_itertools
from irc.bot import ExponentialBackoff, ServerSpec
from irc.dict import IRCDict


class Timmy(irc.client_aio.AioSimpleIRCClient):
    def __init__(self, server_list, nickname, realname, recon=ExponentialBackoff(), **connect_params):
        super(Timmy, self).__init__()

        self.__connect_params = connect_params
        self.channels = IRCDict()

        specs = map(ServerSpec.ensure, server_list)

        self.servers = more_itertools.peekable(itertools.cycle(specs))
        self.recon = recon

        self._nickname = nickname
        self._realname = realname

        for i in [
            "disconnect",
            "join",
            "kick",
            "namreply",
            "nick",
            "part",
            "quit",
        ]:
            self.connection.add_global_handler(i, getattr(self, "_on_" + i), -20)

    def _connect(self):
        server = self.servers.peek()
        try:
            self.connect(
                server.host,
                server.port,
                self._nickname,
                server.password,
                ircname=self._realname,
                **self.__connect_params
            )
            except irc.client.Ser