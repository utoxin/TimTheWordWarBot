from timmy import db_access
from timmy.data.word_war import WordWar


class WordWarDb:
    def __init__(self):
        self.db = None

    def init(self):
        self.db = db_access.connection_pool

    def create_war(self, war: WordWar):
        create_query = "INSERT INTO `wars` (`year`, `uuid`, `channel`, `starter`, `name`, `base_duration`, " \
                       "`base_break`, `total_chains`, `current_chain`, `start_epoch`, `end_epoch`, `randomness`, " \
                       "`war_state`, `created`) VALUES (%(year)s, %(uuid)s, %(channel)s, %(starter)s, %(name)s, " \
                       "%(base_duration)s, %(base_break)s, %(total_chains)s, %(current_chain)s, %(start_epoch)s, " \
                       "%(end_epoch)s, %(randomness)s, %(state)s, NOW())"

        conn = self.db.get_connection()
        cur = conn.cursor()
        cur.execute(create_query, war.data_export())
        war.war_id = cur.lastrowid
        cur.close()
        conn.close()

    def update_war(self, war: WordWar):
        update_query = "UPDATE `wars` SET `current_chain` = %(current_chain)s, `start_epoch` = %(start_epoch)s, " \
                       "end_epoch = %(end_epoch)s, `war_state` = %(state)s WHERE `uuid` = %(uuid)s"

        conn = self.db.get_connection()
        cur = conn.cursor()
        cur.execute(update_query, war.data_export())
        cur.close()
        conn.close()

        self.save_war_members(war)

    def save_war_members(self, war: WordWar):
        delete_statement = "DELETE FROM `war_members` WHERE `war_uuid` = %(uuid)s"
        insert_statement = "INSERT INTO `war_members` SET `war_uuid` = %(uuid)s, `nick` = %(nick)s"

        connection = self.db.get_connection()
        cursor = connection.cursor()

        cursor.execute(delete_statement, {'uuid': str(war.uuid)})

        for nick in war.war_members:
            cursor.execute(insert_statement, {'uuid': str(war.uuid), 'nick': nick})

        connection.close()

    def load_war_members(self, war: WordWar):
        select_statement = "SELECT * FROM `war_members` WHERE `war_uuid` = %(uuid)s"

        connection = self.db.get_connection()
        cursor = connection.cursor(dictionary=True)

        war_members = set()

        cursor.execute(select_statement, {'uuid': str(war.uuid)})
        for row in cursor:
            war_members.add(row['nick'])

        connection.close()

        return war_members

    def load_wars(self):
        select_all_statement = "SELECT * FROM `wars` WHERE `war_state` NOT IN ('CANCELLED', 'FINISHED')"

        wars = []

        connection = self.db.get_connection()
        cursor = connection.cursor(dictionary=True)
        cursor.execute(select_all_statement)
        for row in cursor:
            war = WordWar()
            war.load_from_db(row)
            war.war_members = self.load_war_members(war)
            wars.append(war)

        connection.close()

        return wars

    def load_war_by_id(self, war_id):
        select_statement = "SELECT * FROM `wars` WHERE CONCAT(`year`, '-', `war_id`) = %(war_id)s"

        connection = self.db.get_connection()
        cursor = connection.cursor(dictionary=True)
        cursor.execute(select_statement, {'war_id': war_id})

        war = None

        for row in cursor:
            war = WordWar()
            war.load_from_db(row)

        connection.close()

        return war
