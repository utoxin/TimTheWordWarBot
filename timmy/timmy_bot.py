from timmy.core import logging, bot_instance
from yoyo import read_migrations, get_backend
import os
import sys

if __name__ == "__main__":
    import configparser

    logging.setup_console_logger()

    # Load config
    config = configparser.ConfigParser()
    config.read('botconfig.ini')

    # No config db_access found. Save out a template file, and exit.
    if len(config.sections()) == 0:
        import pathlib

        print(pathlib.Path().resolve())

        print("No config db_access found. Template file created as botconfig.ini. If the default values don't "
              "function, edit and restart the bot.")

        config.add_section("DB")
        config.set("DB", "host", "timmy-db")
        config.set("DB", "port", "3306")
        config.set("DB", "database", "timmy")
        config.set("DB", "user", "timmy")
        config.set("DB", "password", "password")

        with open('botconfig.ini', 'w') as configfile:
            config.write(configfile)

    yoyo_backend = get_backend('mysql://' + config.get("DB", "user") + ':' + config.get("DB", "password") + '@'
                               + config.get("DB", "host") + ":" + config.get("DB", "port") + "/" + config.get("DB", "database"))
    yoyo_migrations = read_migrations(os.path.dirname(os.path.realpath(sys.argv[0])) + '/migrations')

    with yoyo_backend.lock():
        yoyo_backend.apply_migrations(yoyo_backend.to_apply(yoyo_migrations))

    bot_instance.setup(
            config.get("DB", "host"),
            config.get("DB", "database"),
            config.get("DB", "user"),
            config.get("DB", "password"),
            config.get("DB", "port")
    )

    bot_instance.start()
