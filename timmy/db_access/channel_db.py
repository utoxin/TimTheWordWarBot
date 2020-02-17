import time

from timmy import db_access
from timmy.data.channel_data import ChannelData


class ChannelDb:
    def __init__(self):
        self.db = db_access.connection_pool

    def get_channel_list(self):
        select_statement = "SELECT `channel` FROM `channels` WHERE `active` = 1"

        connection = self.db.get_connection()

        channels = []

        cursor = connection.cursor()
        cursor.execute(select_statement)

        for channel in cursor:
            channels.append(channel[0])

        return channels

    def join_channel(self, channel: ChannelData):
        insert_statement = "INSERT INTO `channels` (`channel`) VALUES (%(channel)s) ON DUPLICATE KEY UPDATE " \
                           "`active` = 1"

        connection = self.db.get_connection()

        insert_cursor = connection.cursor()
        insert_cursor.execute(insert_statement, {'channel': channel.name})
        insert_cursor.close()

        connection.close()

        self.load_channel_data(channel)

    def deactivate_channel(self, channel: ChannelData):
        update_statement = "UPDATE `channels` SET `active` = 0 WHERE `channel` = %(channel)s"

        conn = self.db.get_connection()
        cur = conn.cursor()
        cur.execute(update_statement, {'channel': channel.name})
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

        connection = self.db.get_connection()
        cursor = connection.cursor()
        cursor.execute(update_statement, {
            'reactive_chatter_level':   channel.chatter_settings['reactive_level'],
            'chatter_name_multiplier':  channel.chatter_settings['name_multiplier'],
            'random_chatter_level':     channel.chatter_settings['random_level'],
            'tweet_bucket_max':         channel.twitter_settings['bucket_max'],
            'tweet_bucket_charge_rate': channel.twitter_settings['bucket_charge_rate'],
            'auto_muzzle_wars':         channel.auto_muzzle,
            'velociraptor_sightings':   channel.raptor_data['sightings'],
            'active_velociraptors':     channel.raptor_data['active'],
            'dead_velociraptors':       channel.raptor_data['dead'],
            'killed_velociraptors':     channel.raptor_data['killed'],
            'muzzle_expiration':        int(channel.muzzled_until) if channel.muzzled else None,
            'raptor_strength_boost':    channel.raptor_data['strength'],
            'channel':                  channel.name
        })

        delete_chatter_statement = "DELETE FROM `channel_chatter_settings` WHERE `channel` = %(channel)s"
        delete_command_statement = "DELETE FROM `channel_command_settings` WHERE `channel` = %(channel)s"
        delete_twitter_statement = "DELETE FROM `channel_twitter_feeds` WHERE `channel` = %(channel)s"

        cursor.execute(delete_chatter_statement, {'channel': channel.name})
        cursor.execute(delete_command_statement, {'channel': channel.name})
        cursor.execute(delete_twitter_statement, {'channel': channel.name})

        insert_chatter_statement = "INSERT INTO `channel_chatter_settings` SET `channel` = %(channel)s, `setting` = " \
                                   "%(setting)s, `value` = %(value)s"
        insert_command_statement = "INSERT INTO `channel_command_settings` SET `channel` = %(channel)s, `setting` = " \
                                   "%(setting)s, `value` = %(value)s"
        insert_twitter_statement = "INSERT INTO `channel_twitter_feeds` SET `channel` = %(channel)s, `account` = " \
                                   "%(account)s"

        for key, value in channel.chatter_settings['types'].items():
            cursor.execute(insert_chatter_statement, {
                'channel': channel.name,
                'setting': str(key),
                'value':   int(value)
            })

        for key, value in channel.command_settings.items():
            cursor.execute(insert_command_statement, {
                'channel': channel.name,
                'setting': str(key),
                'value':   int(value)
            })

        for account in channel.twitter_accounts:
            cursor.execute(insert_twitter_statement, {
                'channel': channel.name,
                'account': str(account)
            })

    def load_channel_data(self, channel: ChannelData):
        select_channel_statement = "SELECT * FROM `channels` WHERE `channel` = %(channel)s"
        select_chatter_statement = "SELECT `setting`, `value` FROM `channel_chatter_settings` WHERE `channel` = " \
                                   "%(channel)s"
        select_command_statement = "SELECT `setting`, `value` FROM `channel_command_settings` WHERE `channel` = " \
                                   "%(channel)s"
        select_twitter_statement = "SELECT `account` FROM `channel_twitter_feeds` WHERE `channel` = %(channel)s"

        connection = self.db.get_connection()
        select_cursor = connection.cursor(dictionary=True)

        select_cursor.execute(select_channel_statement, {'channel': channel.name})
        for row in select_cursor:
            channel.set_defaults()
            channel.chatter_settings['reactive_level'] = row['reactive_chatter_level']
            channel.chatter_settings['random_level'] = row['random_chatter_level']
            channel.chatter_settings['name_multiplier'] = row['chatter_name_multiplier']

            channel.twitter_settings['bucket_max'] = row['tweet_bucket_max']
            channel.twitter_settings['bucket_charge_rate'] = row['tweet_bucket_charge_rate']

            channel.raptor_data['sightings'] = row['velociraptor_sightings']
            channel.raptor_data['active'] = row['active_velociraptors']
            channel.raptor_data['dead'] = row['dead_velociraptors']
            channel.raptor_data['killed'] = row['killed_velociraptors']
            channel.raptor_data['strength'] = row['raptor_strength_boost']

            channel.auto_muzzle = row['auto_muzzle_wars'] == 1
            channel.muzzled_until = row['muzzle_expiration']
            if channel.muzzled_until is not None and channel.muzzled_until > time.time():
                channel.muzzled = True

        select_cursor.execute(select_chatter_statement, {'channel': channel.name})
        for row in select_cursor:
            channel.chatter_settings['types'][row['setting']] = row['value'] == 1

        select_cursor.execute(select_command_statement, {'channel': channel.name})
        for row in select_cursor:
            channel.command_settings[row['setting']] = row['value'] == 1

        select_cursor.execute(select_twitter_statement, {'channel': channel.name})
        for row in select_cursor:
            channel.twitter_accounts.append(row['account'])

        self.save_channel_settings(channel)
