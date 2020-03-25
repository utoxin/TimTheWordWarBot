import random
import re

from timmy import core
from timmy.data.channel_data import ChannelData
from timmy.utilities import markov_processor, text_generator


class ReactionHandler:
    def on_action(self, connection, event):
        if core.user_perms.is_ignored(event.source.nick, 'soft'):
            return True

        # TODO: Deal with color codes

        channel: ChannelData = core.bot_instance.channels[event.target]

        if channel.is_muzzled():
            return

        self._update_odds(channel)

        # TODO: Add user interaction control checks
        interacted = False

        if channel.chatter_settings['types']['silly_reactions']:
            if "how many lights" in event.arguments[0].lower() \
                    and random.randrange(100) < channel.current_odds['lights']:
                channel.current_odds['lights'] -= 1
                channel.send_message("There are FOUR lights!")
                interacted = True

            elif "what does the fox say" in event.arguments[0].lower() \
                    and random.randrange(100) < channel.current_odds['fox']:
                channel.current_odds['fox'] -= 1
                channel.send_action("mutters under his breath. \"Foxes don't talk. Sheesh.\"")
                interacted = True

            elif "when will then be now" in event.arguments[0].lower() \
                    and random.randrange(100) < channel.current_odds['soon']:
                channel.current_odds['soon'] -= 1
                channel.send_action("replies with certainty. \"Soon.\"")
                interacted = True

            elif "cheeseburger" in event.arguments[0].lower() \
                    and random.randrange(100) < channel.current_odds['cheeseburger']:
                channel.current_odds['cheeseburger'] -= 1
                channel.send_action("sniffs the air, and peers around. \"I can haz cheezburger?\"")
                interacted = True

            elif (":(" in event.arguments[0] or "):" in event.arguments[0]) \
                    and random.randrange(100) < channel.current_odds['hug']:
                channel.current_odds['hug'] -= 1
                channel.send_action("gives {} a hug.".format(event.source.nick))
                interacted = True

            elif event.arguments[0].lower().startswith("tests") \
                    and random.randrange(100) < channel.current_odds['test']:
                channel.current_odds['test'] -= 1
                channel.send_action("considers, and gives {} a grade: {}".format(event.source.nick, self._pick_grade()))
                interacted = True

            elif ":'(" in event.arguments[0] and random.randrange(100) < channel.current_odds['tissue']:
                channel.current_odds['tissue'] -= 1
                channel.send_action("passes {} a tissue.".format(event.source.nick))
                interacted = True

            elif re.search("are you (thinking|pondering) what i.*m (thinking|pondering)", event.arguments[0],
                           re.IGNORECASE) and random.randrange(100) < channel.current_odds['aypwip']:
                channel.current_odds['aypwip'] -= 1
                channel.send_message(text_generator.get_string("[aypwip]", {'target': event.source.nick}))
                interacted = True

            elif re.search("what.*is.*the.*answer", event.arguments[0], re.IGNORECASE) \
                    and random.randrange(100) < channel.current_odds['answer']:
                channel.current_odds['answer'] -= 1
                channel.send_action("sighs at the question. \"The answer is 42. I thought you knew that...\"".format(
                        event.source.nick
                ))
                interacted = True

            elif re.search("{}.*[?]".format(core.bot_instance.connection.nickname), event.arguments[0], re.IGNORECASE) \
                    and random.randrange(100) < channel.current_odds['eightball']:
                channel.current_odds['eightball'] -= 1
                channel.send_message(text_generator.get_string("[eightball]"))
                interacted = True

        if channel.chatter_settings['types']['groot']:
            if "groot" in event.arguments[0].lower() and random.randrange(100) < channel.current_odds['groot']:
                channel.current_odds['groot'] -= 1
                channel.send_action("mutters, \"I am groot.\"")
                # TODO: Vary punctuation, using a list
                interacted = True

        if channel.chatter_settings['types']['velociraptor']:
            if "raptor" in event.arguments[0].lower() and random.randrange(100) < channel.current_odds['velociraptor']:
                channel.current_odds['velociraptor'] -= 1
                core.raptor_ticker.sighting(connection, event)
                interacted = True

        if channel.chatter_settings['types']['helpful_reactions']:
            if re.search("how do (i|you) (change|set) ?(my|your)? (nick|name)", event.arguments[0], re.IGNORECASE):
                channel.send_message("{}: To change your name type the following, putting the name you want instead of "
                                     "NewNameHere: /nick NewNameHere".format(event.source.nick))
                interacted = True

        if not interacted:
            self._interact(connection, event)
            
        markov_processor.store_line("emote", event.arguments[0])

    def on_privmsg(self, connection, event):
        self.on_pubmsg(connection, event)

    def on_pubmsg(self, connection, event):
        if core.user_perms.is_ignored(event.source.nick, 'soft'):
            return True

        # TODO: Deal with color codes

        channel: ChannelData = core.bot_instance.channels[event.target]

        if channel.is_muzzled():
            return

        self._update_odds(channel)

        # TODO: Add user interaction control checks
        interacted = False

        if channel.chatter_settings['types']['silly_reactions']:
            if "how many lights" in event.arguments[0].lower() \
                    and random.randrange(100) < channel.current_odds['lights']:
                channel.current_odds['lights'] -= 1
                channel.send_message("There are FOUR lights!")
                interacted = True

            elif "what does the fox say" in event.arguments[0].lower() \
                    and random.randrange(100) < channel.current_odds['fox']:
                channel.current_odds['fox'] -= 1
                channel.send_message("Foxes don't talk. Sheesh.")
                interacted = True

            elif "when will then be now" in event.arguments[0].lower() \
                    and random.randrange(100) < channel.current_odds['soon']:
                channel.current_odds['soon'] -= 1
                channel.send_message("Soon.")
                interacted = True

            elif "cheeseburger" in event.arguments[0].lower() \
                    and random.randrange(100) < channel.current_odds['cheeseburger']:
                channel.current_odds['cheeseburger'] -= 1
                channel.send_message("I can haz cheezburger?")
                interacted = True

            elif (":(" in event.arguments[0] or "):" in event.arguments[0]) \
                    and random.randrange(100) < channel.current_odds['hug']:
                channel.current_odds['hug'] -= 1
                channel.send_action("gives {} a hug.".format(event.source.nick))
                interacted = True

            elif event.arguments[0].lower().startswith("tests") \
                    and random.randrange(100) < channel.current_odds['test']:
                channel.current_odds['test'] -= 1
                channel.send_message("{}: After due consideration, your test earned a: {}".format(event.source.nick,
                                                                                                  self._pick_grade()))
                interacted = True

            elif ":'(" in event.arguments[0] and random.randrange(100) < channel.current_odds['tissue']:
                channel.current_odds['tissue'] -= 1
                channel.send_action("passes {} a tissue.".format(event.source.nick))
                interacted = True

            elif re.search("are you (thinking|pondering) what i.*m (thinking|pondering)", event.arguments[0],
                           re.IGNORECASE) and random.randrange(100) < channel.current_odds['aypwip']:
                channel.current_odds['aypwip'] -= 1
                channel.send_message(text_generator.get_string("[aypwip]", {'target': event.source.nick}))
                interacted = True

            elif re.search("what.*is.*the.*answer", event.arguments[0], re.IGNORECASE) \
                    and random.randrange(100) < channel.current_odds['answer']:
                channel.current_odds['answer'] -= 1
                channel.send_message("The answer is 42. Everyone knows that.".format(
                        event.source.nick
                ))
                interacted = True

            elif re.search("{}.*[?]".format(core.bot_instance.connection.nickname), event.arguments[0], re.IGNORECASE) \
                    and random.randrange(100) < channel.current_odds['eightball']:
                channel.current_odds['eightball'] -= 1
                channel.send_message(text_generator.get_string("[eightball]"))
                interacted = True

        if channel.chatter_settings['types']['groot']:
            if "groot" in event.arguments[0].lower() and random.randrange(100) < channel.current_odds['groot']:
                channel.current_odds['groot'] -= 1
                channel.send_message("I am groot.")
                # TODO: Vary punctuation, using a list
                interacted = True

        if channel.chatter_settings['types']['velociraptor']:
            if "raptor" in event.arguments[0].lower() and random.randrange(100) < channel.current_odds['velociraptor']:
                channel.current_odds['velociraptor'] -= 1
                core.raptor_ticker.sighting(connection, event)
                interacted = True

        if channel.chatter_settings['types']['helpful_reactions']:
            if re.search("how do (i|you) (change|set) ?(my|your)? (nick|name)", event.arguments[0], re.IGNORECASE):
                channel.send_message("{}: To change your name type the following, putting the name you want instead of "
                                     "NewNameHere: /nick NewNameHere".format(event.source.nick))
                interacted = True

        if not interacted:
            self._interact(connection, event)

        markov_processor.store_line("say", event.arguments[0])

    @staticmethod
    def _update_odds(channel: ChannelData):
        for key in channel.max_odds.keys():
            if channel.current_odds[key] < channel.max_odds[key] and random.randrange(100) == 0:
                channel.current_odds[key] += 1

        channel.save_data()

    @staticmethod
    def _pick_grade():
        grade = random.normalvariate(75, 15)

        if grade < 60:
            return 'F'
        if grade < 63:
            return 'D-'
        if grade < 67:
            return 'D'
        if grade < 70:
            return 'D+'
        if grade < 73:
            return 'C-'
        if grade < 77:
            return 'C'
        if grade < 80:
            return 'C+'
        if grade < 83:
            return 'B-'
        if grade < 87:
            return 'B'
        if grade < 90:
            return 'B+'
        if grade < 93:
            return 'A-'
        if grade < 97:
            return 'A'

        return 'A+'

    @staticmethod
    def _interact(connection, event):
        channel: ChannelData = core.bot_instance.channels[event.target]

        if channel.chatter_settings['random_level'] <= 0:
            return

        odds = channel.chatter_settings['random_level']

        if core.bot_instance.connection.nickname.lower() in event.arguments[0].lower():
            odds = odds * channel['chatter_settings']['name_multiplier']

        if random.random() * 100 < odds:
            enabled_actions = []

            if channel.chatter_settings['types']['markov']:
                enabled_actions.append('markov')

            if channel.amusement_chatter_available():
                enabled_actions.append('amusement')

            if len(enabled_actions) == 0:
                return

            random.shuffle(enabled_actions)

            action = enabled_actions[0]

            if action == 'markov':
                # TODO: Connect to markov generator
                pass
            elif action == 'amusement':
                # TODO: Connection to amusement actions
                pass
