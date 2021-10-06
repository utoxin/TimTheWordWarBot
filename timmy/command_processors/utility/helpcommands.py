from irc.dict import IRCDict

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.data.command_type import CommandType


class HelpCommands(BaseCommand):
    user_commands = ['help']
    admin_commands = ['help']

    interaction_checks = False
    permitted_checks = False

    def __init__(self):
        self.__help_topics = IRCDict()
        self.__help_topics['admin'] = IRCDict()
        self.__help_topics['user'] = IRCDict()

    def process(self, command_data: CommandData) -> None:
        if command_data.type == CommandType.TIMMY_USER:
            self.handle_user_help(command_data)
        elif command_data.type == CommandType.TIMMY_ADMIN:
            self.handle_admin_help(command_data)

    def handle_user_help(self, command_data: CommandData) -> None:
        self.send_action(command_data, f"whispers something to {command_data.issuer}. (Check for a new window or tab "
                                       f"with the help text.)")

        self.pm_user(command_data, "I am a robot trained by the WordWar Monks of Honolulu. You have never heard of "
                                   "them. It is because they are awesome.")

        self.__send_all_topics(command_data, 'user')

        self.pm_user(command_data, "I... I think there might be other tricks I know... You'll have to find them!")
        self.pm_user(command_data, "I will also respond to the /invite command if you would like to see me in another "
                                   "channel.")

    def handle_admin_help(self, command_data: CommandData) -> None:
        self.send_action(command_data, f"whispers something to {command_data.issuer}. (Check for a new window or tab "
                                       f"with the help text.)")

        self.__send_all_topics(command_data, 'admin')

    def __send_all_topics(self, command_data: CommandData, user_type: str) -> None:
        if 'general commands' in self.__help_topics[user_type]:
            self.__send_help_category(command_data, 'general commands',
                                      self.__help_topics[user_type]['general commands'])

        for k, v in sorted(self.__help_topics[user_type].items()):
            if k == 'general commands':
                continue

            self.__send_help_category(command_data, k, v)

    def __send_help_category(self, command_data: CommandData, category: str, category_topics: IRCDict) -> None:
        self.pm_user(command_data, f"{category.title()}:")

        for k, v in sorted(category_topics.items()):
            self.pm_user(command_data, f"    {k} - {v}")

    def add_help_topic(self, user_type: str, category: str, keyword: str, description: str) -> None:
        if user_type not in self.__help_topics:
            return

        if category not in self.__help_topics[user_type]:
            self.__help_topics[user_type][category] = IRCDict()

        self.__help_topics[user_type][category][keyword] = description
