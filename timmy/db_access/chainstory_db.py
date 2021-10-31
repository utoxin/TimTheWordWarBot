from typing import List

from timmy import db_access


class ChainStoryDb:
    def __init__(self):
        self.db = None

    def init(self) -> None:
        self.db = db_access.connection_pool

    def get_last_lines(self) -> List[str]:
        select_statement = "SELECT * FROM `story` WHERE YEAR(`created`) = YEAR(NOW()) ORDER BY id DESC LIMIT 3"

        connection = self.db.get_connection()

        paragraphs = []

        cursor = connection.cursor()
        cursor.execute(select_statement)

        for channel in cursor:
            paragraphs.append(channel[1])

        self.db.close_connection(connection)

        paragraphs.reverse()

        return paragraphs

    def add_line(self, new_line: str, author: str) -> None:
        insert_statement = "INSERT INTO `story` (`string`, `author`, `created`) VALUES (%(new_line)s, %(author)s, " \
                           "NOW())"

        connection = self.db.get_connection()

        insert_cursor = connection.cursor()
        insert_cursor.execute(insert_statement, {'new_line': new_line, 'author': author})
        insert_cursor.close()

        self.db.close_connection(connection)

    def word_count(self) -> int:
        select_statement = "SELECT IFNULL(SUM( LENGTH( STRING ) - LENGTH( REPLACE( STRING ,  ' ',  '' ) ) +1 ), 0) " \
                           "AS word_count FROM story WHERE YEAR(`created`) = YEAR(NOW())"

        connection = self.db.get_connection()

        count = 0

        cursor = connection.cursor()
        cursor.execute(select_statement)

        for record in cursor:
            count = record[0]

        self.db.close_connection(connection)

        return count

    def author_count(self) -> int:
        select_statement = "SELECT COUNT(DISTINCT author) AS author_count FROM story WHERE YEAR(`created`) = " \
                           "YEAR(NOW())"
        connection = self.db.get_connection()

        count = 0

        cursor = connection.cursor()
        cursor.execute(select_statement)

        for record in cursor:
            count = record[0]

        self.db.close_connection(connection)

        return count
