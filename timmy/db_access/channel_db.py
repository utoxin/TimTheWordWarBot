import time

from timmy import db_access
from timmy.data.channel_data import ChannelData


class ChannelDb:
    def __init__(self):
        self.db = db_access.connection_pool

    def join_channel(self, channel: ChannelData):
        select_statement = "SELECT * FROM `channels` WHERE `channel` = %(channel)s"
        insert_statement = "INSERT INTO `channels` (`channel`) VALUES (%(channel)s)"
        update_statement = "UPDATE `channels` SET `active` = 1 WHERE `channel` = %(channel)s"

        connection = self.db.get_connection()

        select_cursor = connection.cursor()
        select_cursor.execute(select_statement, {'channel': channel.channel})

        if select_cursor.rowcount > 0:
            update_cursor = connection.cursor()
            update_cursor.execute(update_statement, {'channel': channel.channel})
            update_cursor.close()

        else:
            insert_cursor = connection.cursor()
            insert_cursor.execute(insert_statement, {'channel': channel.channel})
            insert_cursor.close()

        select_cursor.close()
        connection.close()

    def deactivate_channel(self, channel: ChannelData):
        update_statement = "UPDATE `channels` SET `active` = 0 WHERE `channel` = %(channel)s"

        conn = self.db.get_connection()
        cur = conn.cursor()
        cur.execute(update_statement, {'channel': channel.channel})
        cur.close()
        conn.close()

    def save_channel_settings(self, channel: ChannelData):
        update_statement = "UPDATE `channels` SET `reactive_chatter_level` = %(reactive_chatter_level)s, " \
                           "`chatter_name_multiplier` = %(chatter_name_multiplier)s, `random_chatter_level` = " \
                           "%(random_chatter_level)s, `tweet_bucket_max` = %(tweet_bucket_max)s, " \
                           "`tweet_bucket_charge_rate` = %(tweet_bucket_charge_rate)s, `auto_muzzle_wars` = " \
                           "%(auto_muzzle_wars)s, `velociraptor_sightings` = %(velociraptor_sightings)s, " \
                           "`active_velociraptors` = %(active_velociraptors)s, `dead_velociraptors` = " \
                           "%(dead_velociraptors)s, `killed_velociraptors` = %(killed_velociraptors)s, " \
                           "`muzzle_expiration` = %(muzzle_expiration)s, `raptor_strength_boost` = " \
                           "%(raptor_strength_boost)s WHERE `channel` = %(channel)s"

        conn = self.db.get_connection()
        cur = conn.cursor()
        cur.execute(update_statement, {
            'reactive_chatter_level': channel.chatter_settings['reactive_level'],
            'chatter_name_multiplier': channel.chatter_settings['name_multiplier'],
            'random_chatter_level': channel.chatter_settings['random_level'],
            'tweet_bucket_max': channel.twitter_settings['bucket_max'],
            'tweet_bucket_charge_rate': channel.twitter_settings['bucket_charge_rate'],
            'auto_muzzle_wars': channel.auto_muzzle,
            'velociraptor_sightings': channel.raptor_data['sightings'],
            'active_velociraptors': channel.raptor_data['active'],
            'dead_velociraptors': channel.raptor_data['dead'],
            'killed_velociraptors': channel.raptor_data['killed'],
            'muzzle_expiration': channel.muzzled_until if channel.muzzled else None,
            'raptor_strength_boost': channel.raptor_data['strength'],
            'channel': channel.channel
        })

    def load_channel_data(self, channel: ChannelData):
        select_channel_statement = "SELECT * FROM `channels` WHERE `channel` = %(channel)s"
        select_chatter_statement = "SELECT `setting`, `value` FROM `channel_chatter_settings` WHERE `channel` = " \
                                   "%(channel)s"
        select_command_statement = "SELECT `setting`, `value` FROM `channel_command_settings` WHERE `channel` = " \
                                   "%(channel)s"
        select_twitter_statement = "SELECT `account` FROM `channel_twitter_settings` WHERE `channel` = %(channel)s"

        connection = self.db.get_connection()

        select_cursor = connection.cursor(dictionary=True)
        select_cursor.execute(select_channel_statement, {'channel': channel.channel})

        if select_cursor.rowcount:
            select_data = select_cursor.fetchone()

            channel.set_defaults()
            channel.chatter_settings['reactive_level'] = select_data['reactive_chatter_level']
            channel.chatter_settings['random_level'] = select_data['random_chatter_level']
            channel.chatter_settings['name_multiplier'] = select_data['chatter_name_multiplier']

            channel.twitter_settings['bucket_max'] = select_data['tweet_bucket_max']
            channel.twitter_settings['bucket_charge_rate'] = select_data['tweet_bucket_charge_rate']

            channel.raptor_data['sightings'] = select_data['velociraptor_sightings']
            channel.raptor_data['active'] = select_data['active_velociraptors']
            channel.raptor_data['dead'] = select_data['dead_velociraptors']
            channel.raptor_data['killed'] = select_data['killed_velociraptors']
            channel.raptor_data['strength'] = select_data['raptor_strength_boost']

            channel.auto_muzzle = select_data['auto_muzzle'] == 1
            channel.muzzled_until = select_data['muzzle_expiration']
            if channel.muzzled_until > time.time():
                channel.muzzled = True

            