"""
add 'commandment' command strings to pychance
"""
import uuid

from pymysql.connections import Connection
from yoyo import step

__depends__ = {'20210817_01_S9xo7-add-catch-command-strings-to-pychance'}

entries_to_add = [
    ['commandment', [
        "[commandment1]", "[commandment2]", "[commandment3]", "[commandment4]", "[commandment5]", "[commandment6]",
        "[commandment7]", "[commandment8]", "[commandment9]", "[commandment10]", "[commandment11]", "[commandment12]",
        "[commandment13]",
    ]],
    ['commandment1', [
        "1. Thou shalt not edit during the Holy Month.",
    ]],
    ['commandment2', [
        "2. Thou shalt daily offer up at least 1,667 words to the altar of Chris T. Baty.",
    ]],
    ['commandment3', [
        "3. Keep thou holy the first and last days of the Holy Month, which is November.",
    ]],
    ['commandment4', [
        "4. Take not the name of Chris T. Baty in vain, unless it doth provide thee with greater word count, which "
        "is good.",
    ]],
    ['commandment5', [
        "5. Worry not about the quality of thy words, for Chris T. Baty cares not. Quantity is that which pleases Baty.",
    ]],
    ['commandment6', [
        "6. Thou must tell others of the way of the truth, by leading by example in your region.",
    ]],
    ['commandment7', [
        "7. Honor thou those who sacrifice their time. They are known as MLs and Staff members, and they are blessed.",
    ]],
    ['commandment8', [
        "8. Once in your life, ye shall make a pilgrimage to NOWD to honor thine Chris T. Baty",
    ]],
    ['commandment9', [
        "9. Those that sacrifice their money shall be blessed with gold, which shall be a sign unto others.",
    ]],
    ['commandment10', [
        "10. <<WRITE THIS LATER>>",
    ]],
    ['commandment11', [
        "11. Thou shalt back up thy writing often, for it is displeasing in the eyes of Baty that you should lose it.",
    ]],
    ['commandment12', [
        "12. No narrative? No botheration!",
    ]],
    ['commandment13', [
        "13. Thou shalt not break the MLs."
    ]],
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
