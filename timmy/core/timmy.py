from timmy import db_access


class Timmy:
    def __init__(self):
        self.connections = ()

    def setup(self, host: str, database: str, user: str, password: str, port: int, encrypt_passphrase: str) -> None:
        db_access.init_db_access()

        pool = db_access.connection_pool
        pool.setup(host, database, user, password, port)

        settings = db_access.settings

        self._nickname = settings.get_setting("nickname")
        self._realname = settings.get_setting("realname")

        server = ServerSpec(settings.get_setting("server"))
        specs = map(ServerSpec.ensure, [server])

        self.servers = more_itertools.peekable(itertools.cycle(specs))
        self.recon = ExponentialBackoff()

    def start(self) -> None:
        return
