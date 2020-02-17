from timmy import db_access


class WordWarDb:
    def __init__(self):
        self.db = db_access.connection_pool

    def create_war(self, war):
        create_query = "INSERT INTO `new_wars` (`year`, `uuid`, `channel`, `starter`, `name`, `base_duration`, " \
                       "`base_break`, `total_chains`, `current_chain`, `start_epoch`, `end_epoch`, `randomness`, " \
                       "`war_state`, `created`) VALUES (%(year)s, %(uuid)s, %(channel)s, %(starter)s, %(name)s, " \
                       "%(base_duration)s, %(base_break)s, %(total_chains)s, %(current_chain)s, %(start_epoch)s, " \
                       "%(end_epoch)s, %(randomness)s, %(war_state)s, NOW())"

        conn = self.db.get_connection()
        cur = conn.cursor()
        cur.execute(create_query, war)
        war_id = cur.lastrowid
        cur.close()
        conn.close()

        return war_id

    def update_war(self, war):
        update_query = "UPDATE `new_wars` SET `current_chain` = %(current_chain)s, `start_epoch` = %(start_epoch)s, " \
                       "end_epoch = %(end_epoch)s, `war_state` = %(war_state)s WHERE `uuid` = %(uuid)s"

        conn = self.db.get_connection()
        cur = conn.cursor()
        cur.execute(update_query, war)
        cur.close()
        conn.close()
