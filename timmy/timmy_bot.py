from timmy.core import bot_instance, logging

if __name__ == "__main__":
    import configparser

    logging.setup_console_logger()

    # Load config
    config = configparser.ConfigParser()
    config.read('../botconfig.ini')

    # No config db_access found. Save out a template file, and exit.
    if len(config.sections()) == 0:
        print("No config db_access found. Template file created as botconfig.ini. Edit file and restart.")

        config.add_section("DB")
        config.set("DB", "host", "localhost")
        config.set("DB", "database", "timmy")
        config.set("DB", "user", "timmy")
        config.set("DB", "password", "password")

        with open('../botconfig.ini', 'w') as configfile:
            config.write(configfile)

        exit(1)

    bot_instance.setup(
            config.get("DB", "host"),
            config.get("DB", "database"),
            config.get("DB", "user"),
            config.get("DB", "password")
    )

    bot_instance.start()
