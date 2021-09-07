import random
import re
import threading

from irc.client import Event, ServerConnection

from timmy import core
from timmy.data.channel_data import ChannelData
from timmy.utilities import text_generator


class ServerHandler:
    def on_invite(self, connection: ServerConnection, event: Event) -> None:
        x = threading.Thread(target = self._on_invite, args = (connection, event), name = "ServerHandler-on_invite")
        x.start()

    @staticmethod
    def _on_invite(connection: ServerConnection, event: Event) -> None:
        connection.join(event.arguments[0])

    def on_join(self, connection: ServerConnection, event: Event) -> None:
        x = threading.Thread(target = self._on_join, args = (connection, event), name = "ServerHandler-on_join")
        x.start()

    @staticmethod
    def _on_join(connection: ServerConnection, event: Event) -> None:
        channel: ChannelData = core.bot_instance.channels[event.target]
        nick = event.source.nick

        if nick == connection.get_nickname():
            from timmy.core import war_ticker
            war_ticker.activate_channel_wars(channel)
            return

        if core.user_perms.is_ignored(nick, 'any'):
            return

        if channel.chatter_settings['types']['silly_reactions']:
            connection.privmsg(channel.name, text_generator.get_string('[greeting]', {'target': nick}))

            if random.randrange(100) < 15:
                if random.getrandbits(1) == 1:
                    connection.privmsg(channel.name, text_generator.get_string('[extra_greeting]'))
                else:
                    velociraptor_count = channel.raptor_data['active']
                    plural = "" if velociraptor_count == 1 else "s"

                    connection.privmsg(
                            channel.name, f"This channel has a population of {velociraptor_count:n} "
                                          f"velociraptor{plural}!"
                    )

        if channel.chatter_settings['types']['helpful_reactions']:
            channel_wars = [w for w in core.war_ticker.active_wars if w.channel.lower() == channel.name.lower()]
            war_count = len(channel_wars)

            if war_count > 0:
                plural = "" if war_count < 2 else "s"
                are = "is" if war_count < 2 else "are"

                connection.privmsg(
                        channel.name, "There {} {:n} war{} currently running in this channel:".format(
                                are, war_count, plural
                        )
                )

                for war in channel_wars:
                    connection.privmsg(channel.name, war.get_description())

            if re.search('^(mib_(\\d+))|^(guest(\\d+))', nick, re.IGNORECASE):
                connection.privmsg(
                        channel.name, "{}: To change your name, type the following, putting the name you "
                                      "want instead of NewNameHere: /nick NewNameHere".format(nick)
                )
