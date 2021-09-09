"""
add 'summon/banish' strings to pychance
"""
import uuid

from pymysql import Connection
from yoyo import step

__depends__ = {'20210907_01_zIrFD-fix-item-2-tags'}

entries_to_add = [
    ['banish_start', [
        "gathers the supplies necessary to banish [target] to the outer darkness...",
        "looks through an ancient scroll for the procedure to banish [target]...",
        "writes a program called \"NoMore[target].exe\" in an magical programming language...",
        "writes a program called \"Banish[target].exe\" in an arcane programming language...",
    ]],
    ['banish_end', [
        "completes the ritual successfully, banishing [target] to the outer darkness, where they can't interfere "
        "with Timmy's affairs!",
        "completes the ritual to banish [target], but they reappear after a short absence, looking a bit annoyed.",
        "attempts to banish [target], but something goes horribly wrong. As the ritual is completed, [alternate] "
        "appears to chastise [banisher] for his temerity.",
        "fails completely in his attempt to banish [target]. They don't even seem to notice...",
        "succeeds in sending [target] back to their home plane. Unfortunately, that plane is this one.",
        "must have messed up his attempt to banish [target] pretty badly. [banisher] disappears instead."
    ]],
    ['summon_start', [
        "prepares the summoning circle required to bring [target] into the world...",
        "looks through an ancient manuscript for the procedure to summon [target]...",
        "writes a program called \"Summon[target].exe\" in an arcane programming language...",
    ]],
    ['summon_end', [
        "completes the ritual successfully, drawing [target] through, and binding them into the summoning circle!",
        "completes the ritual, drawing [target] through, but something goes wrong and they fade away after just a few "
        "moments.",
        "attempts to summon [target], but something goes horribly wrong. After the smoke clears, [alternate] is left "
        "standing on the smoldering remains of the summoning circle.",
        "tries to figure out where he went wrong. He certainly didn't mean to summon [alternate], and now they're "
        "pretty angry...",
        "succeeds at his attempt to summon [target], but [alternate] came along as well.",
        "fails to summon [target]. But a small note addressed to [summoner] does appear. Unfortunately, it's written "
        "in a lost language..."
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
            cursor.execute(
                    "DELETE pbt, pbte FROM `pychance_basic_tables` pbt INNER JOIN "
                    "`pychance_basic_table_entries` pbte ON (pbt.uuid = pbte.pychance_basic_table_id)"
                    "WHERE pbt.table_name = %s", list_entry[0]
            )


steps = [
    step(add_entries, remove_entries)
]
