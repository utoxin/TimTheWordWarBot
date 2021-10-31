import random

from timmy import db_access


class BoxODoomDb:
    def __init__(self):
        self.db = None

    def init(self) -> None:
        self.db = db_access.connection_pool

    def get_random_challenge(self, difficulty: str) -> float:
        select_statement = "SELECT `challenge` FROM `box_of_doom` WHERE `difficulty` = %(difficulty)s ORDER BY " \
                           "rand() LIMIT 1"

        connection = self.db.get_connection()

        cursor = connection.cursor()
        cursor.execute(select_statement, {'difficulty': difficulty})

        challenge = random.randrange(10, 30)

        for record in cursor:
            challenge = float(record[0])

        self.db.close_connection(connection)

        return challenge
