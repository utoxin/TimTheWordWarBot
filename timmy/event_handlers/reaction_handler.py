from timmy import core
from timmy.data.channel_data import ChannelData


class ReactionHandler:
    def on_action(self, connection, event):
        if core.user_perms.is_ignored(event.source.nick, 'soft'):
            return True

        channel: ChannelData = core.bot_instance.channels[event.target]

    def on_privmsg(self, connection, event):
        self.on_pubmsg(connection, event)

    def on_pubmsg(self, connection, event):
        if core.user_perms.is_ignored(event.source.nick, 'soft'):
            return True

        channel: ChannelData = core.bot_instance.channels[event.target]
