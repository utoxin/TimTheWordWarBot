from timmy.core import logging, bot_instance

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

    bot_instance.setup(
            config.get("DB", "host"),
            config.get("DB", "database"),
            config.get("DB", "user"),
            config.get("DB", "password"),
            config.get("DB", "port")
    )

    bot_instance.start()
