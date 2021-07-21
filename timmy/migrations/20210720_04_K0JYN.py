"""

"""
import uuid

from pymysql import Connection
from yoyo import step

__depends__ = {'20210720_03_YZtNs'}


def migrate_tables(conn: Connection):
    fetch_lists_sql = "SELECT id, name FROM `lists`"
    fetch_list_items_sql = "SELECT id, list_id, item_name FROM `list_items` WHERE `list_id` = %s"

    insert_list_sql = "INSERT INTO `pychance_basic_tables` (`uuid`, `table_name`) VALUES(%s, %s)"
    insert_list_items_sql = "INSERT INTO `pychance_basic_table_entries` " \
                            "(`uuid`, `pychance_basic_table_id`, `table_entry`) VALUES(%s, %s, %s)"

    with conn.cursor() as cursor:
        cursor.execute(fetch_lists_sql)

        lists = cursor.fetchall()

        for list_entry in lists:
            list_uuid = uuid.uuid4()
            cursor.execute(insert_list_sql, (list_uuid, list_entry[1]))

            cursor.execute(fetch_list_items_sql, (list_entry[0]))

            list_items = cursor.fetchall()

            for list_item in list_items:
                item_uuid = uuid.uuid4()

                cursor.execute(insert_list_items_sql, (item_uuid, list_uuid, list_item[2]))


def rollback_tables(conn: Connection):
    with conn.cursor() as cursor:
        cursor.execute("TRUNCATE TABLE pychance_basic_tables")
        cursor.execute("TRUNCATE TABLE pychance_basic_table_entries")


steps = [
    step(migrate_tables, rollback_tables)
]
