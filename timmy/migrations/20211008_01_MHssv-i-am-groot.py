"""
I am Groot
"""
import uuid

from pymysql import Connection
from yoyo import step

__depends__ = {'20211007_02_tzGvF-fix-raptor-messages-part-2'}

entries_to_add = [
    ['groot', [
        "I am Groot.",
        "I am Groot!",
        "I am Groot?",
        "I am Groot?!",
        "I am Groot...",
        "I. Am. Groot.",
        "[groot] [groot]",
        "[groot] [groot] [groot]"
    ]]
]


def add_entries(conn: Connection):
    insert_list_sql = "INSERT INTO `pychance_basic_tables` (`uuid`, `table_name`) VALUES(%s, %s)"
    insert_list_items_sql = "INSERT INTO `pychance_basic_table_entries` " \
                            "(`uuid`, `pychance_basic_table_id`, `table_entry`) VALUES(%s, %s, %s)"

    with conn.cursor() as cursor:
        for list_entry in entries_to_add:
            list_uuid = uuid.uuid4()
            cursor.execute(insert_list_sql, (list_uuid, list_entry[0]))

            for list_item in list_entry[1]:
                item_uuid = uuid.uuid4()

                cursor.execute(insert_list_items_sql, (item_uuid, list_uuid, list_item))


def remove_entries(conn: Connection):
    with conn.cursor() as cursor:
        for list_entry in entries_to_add:
            cursor.execute(
                    "DELETE pbt, pbte FROM `pychance_basic_tables` pbt INNER JOIN "
                    "`pychance_basic_table_entries` pbte ON (pbt.uuid = pbte.pychance_basic_table_id)"
                    "WHERE pbt.table_name = %s", list_entry[0]
            )


steps = [
    step(add_entries, remove_entries)
]
