"""
add 'catch' command strings to pychance
"""
import uuid

from pymysql.connections import Connection
from yoyo import step

__depends__ = {'20210801_01_asngL-add-default-attack-message'}


entries_to_add = [
    ['catch_initial', [
        "grabs [color] and [color] pokeball, and attempts to catch [target]!",
    ]],
    ['catch_result', [
        "catches [target], and adds them to his collection!",
        "almost catches [target], but they manage to break out of the pokeball and escape!",
        "somehow misses [target], and his pokeball captures [pokemon] instead. Oops!",
        "misses his throw completely, and catches nothing. Awwwww."
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
            cursor.execute("DELETE pbt, pbte FROM `pychance_basic_tables` pbt INNER JOIN "
                           "`pychance_basic_table_entries` pbte ON (pbt.uuid = pbte.pychance_basic_table_id)"
                           "WHERE pbt.table_name = %s", list_entry[0])


steps = [
    step(add_entries, remove_entries)
]
