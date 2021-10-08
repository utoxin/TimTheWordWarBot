"""
Fix Raptor Messages, Part 2
"""
import uuid

from pymysql import Connection
from yoyo import step

__depends__ = {'20211007_01_ohGDB-fix-raptor-messages'}

entries_to_add = [
    ['raptor_sighting_action_response', [
        "jots down a note in a red notebook labeled 'Velociraptor Sighting Log'."
    ]],
    ['raptor_sighting_message_response', [
        "Velociraptor sighted! Incident has been logged."
    ]],
    ['raptor_sighting_old_action_response', [
        "starts to record the new sighting, but then recognizes that raptor from his notes."
    ]],
    ['raptor_sighting_old_message_response', [
        "Velociraptor sight... wait. That's not a new raptor. You need to be more careful about reporting "
        "sightings."
    ]],
    ['raptor_colonize_source_message', [
        "Apparently feeling crowded, [raptor_count] of the velociraptors head off in search of new territory. "
        "After searching, they settle in [destination_channel]."
    ]],
    ['raptor_colonize_destination_message', [
        "A swarm of [raptor_count] velociraptors appears from the direction of [source_channel]. The local raptors "
        "are nervous, but the strangers simply want to join the colony."
    ]],
    ['raptor_brag_origin', [
        "[raptor_name] hurries off to [destination_channel], so they can brag about their person's wordcount.",
        "Inspired by their person's wordcount, [raptor_name] runs off to [destination_channel] to brag."
    ]],
    ['raptor_brag_destination', [
        "[raptor_name] hurries in from [origin_channel], and announces that their person just wrote [word_count] "
        "words.",
        "After arriving from [origin_channel], [raptor_name] announces proudly that their human wrote [word_count] "
        "words."
    ]],
    ['raptor_steal_bunnies_origin_0', [
        "[raptor_name] is running out of ideas, but after raiding [destination_channel] they come back with "
        "[bunny_count] plot bunnies. Sad day.",
    ]],
    ['raptor_steal_bunnies_origin_1', [
        "[raptor_name] needs some ideas, so they run off to [destination_channel] and managed to find "
        "[bunny_count] plot bunny to bring home."
    ]],
    ['raptor_steal_bunnies_origin_2', [
        "[raptor_name] is desperate for ideas, so they charge off to [destination_channel] and come home with "
        "[bunny_count] plot bunnies to add to their collection."
    ]],
    ['raptor_steal_bunnies_destination_0', [
        "A raptor named [raptor_name] charges in from [origin_channel], looking for something. But they miss "
        "seeing the plot bunnies, and leave without any."
    ]],
    ['raptor_steal_bunnies_destination_1', [
        "The raptor [raptor_name] runs in from [origin_channel] and finds a lonely plot bunny, which they throw "
        "in their pack, before heading back home.",
    ]],
    ['raptor_steal_bunnies_destination_2', [
        "A raptor wearing a nametag that says [raptor_name] heads in from the direction of [origin_channel], "
        "throws [bunny_count] plot bunnies into their bag, and then runs off again."
    ]],
    ['raptor_recruit_origin_0', [
        "[raptor_name] hurries off to [destination_channel] and after a short time away comes back, dejected, "
        "without managing to tempt any raptors.",
    ]],
    ['raptor_recruit_origin_1', [
        "[raptor_name] hurries off to [destination_channel] and after a short time away comes back, leading the "
        "single raptor they tempted to join them.",
    ]],
    ['raptor_recruit_origin_2', [
        "[raptor_name] hurries off to [destination_channel] and after a short time away comes back with "
        "[raptor_count] raptors they tempted to join them."
    ]],
    ['raptor_recruit_destination_0', [
        "[raptor_name] slips in from [origin_channel] and makes encouraging noises at the local raptors, but they "
        "have no effect. They leave without any raptors.",
    ]],
    ['raptor_recruit_destination_1', [
        "[raptor_name] sneaks in from [origin_channel] and is approached by one raptor, who they lead off with "
        "them after a few minutes of encouraging noises.",
    ]],
    ['raptor_recruit_destination_2', [
        "[raptor_name] hurries in from [origin_channel] and after a short time, manages to lure [raptor_count] "
        "raptors to follow them back home."
    ]],
    ['raptor_attack_message', [
        "Suddenly, [attack_count] of the raptors go charging off to attack a group in [target_channel]! After "
        "a horrific battle, they manage to kill [kill_count] of them, and [return_count] return home!",
        "A group of [attack_count] local raptors are restless, and head off to [target_channel], where they kill "
        "[kill_count] raptors. Eventually, [return_count] of them make their way back home.",
        "Outraged by previous attacks, [attack_count] of the local raptors head off to [target_channel], killing "
        "[kill_count] others to satisfy their thirst for revenge. Only [return_count] return home."
    ]],
    ['raptor_defense_message', [
        "A swarm of [attack_count] raptors suddenly appears from the direction of [source_channel]. The local "
        "raptors do their best to fight them off, and [kill_count] of them die before the swarm disappears.",
        "A group of [attack_count] restless raptors from [source_channel] shows up without warning, and manage to "
        "kill [kill_count] local raptors before they move on.",
        "In a completely unjustified attack from [source_channel], [attack_count] raptors charge in, savage "
        "[kill_count] of the local raptors, and then run off into the sunset."
    ]],
    ['raptor_hatch_message_1', [
        "Something is going on in the swarm... hey, where did that baby raptor come from?! Clever girls.",
        "You hear an odd chirping sound, and after looking around, discover a newly hatched raptor.",
        "Hold on... where did that baby raptor come from?",
        "You could have sworn that baby raptor wasn't there a few minutes ago.",
        "A female raptor whistles proudly, showing off her freshly hatched child."
    ]],
    ['raptor_hatch_message_2', [
        "Something is going on in the swarm... hey, where did those [raptor_count] baby raptors come from?! Clever "
        "girls.",
        "You hear a chorus of chirps, and it doesn't take long to discover a flock of [raptor_count] baby raptors. "
        "Oh dear.",
        "Back away carefully... there's [raptor_count] baby raptors right there in the corner.",
        "You could have sworn there weren't [raptor_count] baby raptors here a few minutes ago...",
        "You are momentarily deafened by the proud new mothers, arguing over which of the [raptor_count] baby "
        "raptors is cutest."
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
