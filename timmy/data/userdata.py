from timmy import db_access


class UserData:
    def __init__(self):
        self.registration_data_retrieved = False
        self.last_whois_check = None

        self.nicks = set()
        self.recorded_wars = {}

        self.uuid = None
        self.authed_user = ""
        self.total_sprint_wordcount = 0
        self.total_sprints = 0
        self.total_sprint_duration = 0
        self.raptor_adopted = False
        self.raptor_name = ""
        self.raptor_favorite_color = ""
        self.raptor_bunnies_stolen = 0
        self.last_bunny_raid = None

    def save(self):
        update_statement = "REPLACE INTO `users` (`id`, `authed_user`, `total_sprint_wordcount`, `total_sprints`, " \
                           "`total_sprint_duration`, `raptor_adopted`, `raptor_name`, `raptor_favorite_color`, " \
                           "`raptor_bunnies_stolen`, `last_bunny_raid`) VALUES (%(uuid)s, %(authed_user)s, " \
                           "%(total_sprint_wordcount)s, %(total_sprints)s, %(total_sprint_duration)s, " \
                           "%(raptor_adopted)s, %(raptor_name)s, %(raptor_favorite_color)s, " \
                           "%(raptor_bunnies_stolen)s, %(last_bunny_raid)s)"

        connection = db_access.connection_pool.get_connection()
        cursor = connection.cursor()
        cursor.execute(update_statement, {
            'uuid':                   str(self.uuid),
            'authed_user':            self.authed_user,
            'total_sprint_wordcount': self.total_sprint_wordcount,
            'total_sprints':          self.total_sprints,
            'total_sprint_duration':  self.total_sprint_duration,
            'raptor_adopted':         self.raptor_adopted,
            'raptor_name':            self.raptor_name,
            'raptor_favorite_color':  self.raptor_favorite_color,
            'raptor_bunnies_stolen':  self.raptor_bunnies_stolen,
            'last_bunny_raid':        self.last_bunny_raid
        })

        connection.close()
