"""
add 'herd' command strings to pychance
"""
import uuid

from pymysql.connections import Connection
from yoyo import step

__depends__ = {'20210824_01_d87YC-add-commandment-command-strings-to-pychance'}

entries_to_add = [
    ['cat_herd', [
        "herds cats towards [target]. [target] is ambushed by an army of aloof Abyssinians.",
        "herds cats towards [target]. [target] is assaulted by an assembly of assuaged Aegean's.",
        "herds cats towards [target]. [target] is bombarded by a brood of belligerent bobcats.",
        "herds cats towards [target]. [target] is beseiged by a bevy of bellicose Bengals.",
        "herds cats towards [target]. [target] is cornered by a crowd of contentious cheetas.",
        "herds cats towards [target]. [target] is commandeered by a collection of cranky Cornish Rex's.",
        "herds cats towards [target]. [target] is confounded by a colony of cantankerous cougars.",
        "herds crows towards [target]. [target] is corralled by a company of curious crows. (Wait... crows?! Crows "
        "aren't cats! Where did they come from?)",
        "herds cats towards [target]. [target] is capsized by a convoy of combative Calico's.",
        "herds cats towards [target]. [target] is decimated by a drove of devious Dwelfs.",
        "herds cats towards [target]. [target] is floored by a flight of foul fur-balls.",
        "herds cats towards [target]. [target] is flattened by a flock of felonious felines.",
        "herds cats towards [target]. [target] is goosed by a gaggle of gregarious grimalkin.",
        "herds cats towards [target]. [target] is hammered by a herd of horrendous hairballs.",
        "herds cats towards [target]. [target] is lampooned by a legion of lecherous lynxes.",
        "herds cats towards [target]. [target] is harangued by a host of hacked up hairballs.",
        "herds cats towards [target]. [target] is licked by a litter of little LaPerms.",
        "herds cats towards [target]. [target] is muttered at by a mass of marauding Munchkins.",
        "herds cats towards [target]. [target] is mustered by a multitude of murderous mousers.",
        "herds cats towards [target]. [target] is mussed by a murder of malcontent Manxes.",
        "herds cats towards [target]. [target] is purloined by a pack of persnickety Persians.",
        "herds cats towards [target]. [target] is powned by a plethora of purring Pixie-bobs.",
        "herds cats towards [target]. [target] is razed by a rout of rambunctious Ragamuffins.",
        "herds cats towards [target]. [target] is squashed by a score of sinister Siamese.",
        "herds cats towards [target]. [target] is threatened by a throng of terrifying tomcats."
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
