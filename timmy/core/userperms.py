from timmy.core import bot_instance


class UserPerms:
    @staticmethod
    def is_admin(user, channel):
        if user in bot_instance.channels[channel].opers():
            return True

        return False

    @staticmethod
    def is_registered(user):
        return False
