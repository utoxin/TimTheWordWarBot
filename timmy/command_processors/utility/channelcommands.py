from timmy.command_processors.base_command import BaseCommand


class ChannelCommands(BaseCommand):
    admin_commands = {'setmuzzleflag', 'automuzzlewars', 'muzzle', 'chatterlevel', 'chatterflag', 'commandflag',
                      'twitterrelay', 'twitterbucket', 'part'}
