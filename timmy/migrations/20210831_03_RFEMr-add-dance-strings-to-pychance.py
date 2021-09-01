"""
add 'dance' strings to pychance
"""
import uuid

from pymysql.connections import Connection
from yoyo import step

__depends__ = {'20210831_02_v67wR-cleanup-old-string-tables'}


def migrate_tables(conn: Connection):
    fetch_items_sql = "SELECT id, name FROM `dances`"

    insert_list_sql = "INSERT INTO `pychance_basic_tables` (`uuid`, `table_name`) VALUES(%s, %s)"
    insert_items_sql = "INSERT INTO `pychance_basic_table_entries` (`uuid`, `pychance_basic_table_id`, `table_entry`)" \
                       " VALUES(%s, %s, %s)"

    with conn.cursor() as cursor:
        list_uuid = uuid.uuid4()
        cursor.execute(insert_list_sql, (list_uuid, "dance"))

        cursor.execute(fetch_items_sql)

        list_items = cursor.fetchall()

        for list_item in list_items:
            item_uuid = uuid.uuid4()

            cursor.execute(insert_items_sql, (item_uuid, list_uuid, list_item[1]))

        cursor.execute("DROP TABLE dances")


steps = [
    step(migrate_tables)
]
