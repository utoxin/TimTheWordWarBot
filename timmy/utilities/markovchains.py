import random
import re

from timmy import db_access
from timmy.data.channel_data import ChannelData
from timmy.utilities import markov_processor


class MarkovChains:
    def __init__(self):
        self.db = None
        self.ending_punctuation = [".", ".", ".", ".", "!", "!", "?", "?", "!", "!", "?", "?", "...", "?!", "...!",
                                   "...?"]

    def init(self):
        self.db = db_access.connection_pool

    def random_action(self, channel: ChannelData, message_type: str, message: str):
        if message_type == 'mutter':
            output = self._generate_markov('say', message)

            if len(output) > 350:
                output = output[:350] + "..."

            channel.send_action('mutters under his breath, "{}"'.format(output))

        else:
            output = self._generate_markov(message_type, message)

            if len(output) > 400:
                output = output[:400] + "..."

            if message_type == 'say' or message_type == 'novel':
                channel.send_message(output)
            else:
                channel.send_action(output)

    def generate_markov(self, message_type: str, message: str):
        seed_word = 0

        if message != '':
            seed_word = self._get_seed_word(message_type, message, 0)

        return self._generate_from_seedword(message_type, seed_word)

    def _get_seed_word(self, message_type: str, message: str, last_seed: int):
        words = message.split()
        word_ids = set()

        for word in words:
            word_ids.add(markov_processor.get_markov_word_id(word))

        word_ids.remove(last_seed)

        if len(word_ids) == 0:
            return 0

        ids = ",".join(word_ids)

        if message_type == 'say' or message_type == 'emote':
            select_statement = "SELECT * FROM (SELECT second_id FROM markov3_{}_data md WHERE md.first_id = 1 AND " \
                               "md.second_id != 1 AND md.third_id != 1 AND (md.second_id IN ({}) OR md.third_id " \
                               "IN ({})) GROUP BY md.second_id ORDER BY sum(md.count) ASC LIMIT %(limit)s) derived " \
                               "ORDER BY RAND() LIMIT 1".format(message_type, ids, ids)

        elif message_type == 'novel':
            select_statement = "SELECT * FROM (SELECT second_id FROM markov4_{}_data md WHERE md.first_id = 1 AND " \
                               "md.second_id != 1 AND md.third_id != 1 AND md.fourth_id != 1 AND (md.second_id IN " \
                               "({}) OR md.third_id IN ({}) OR md.fourth_id IN ({})) GROUP BY md.second_id ORDER BY " \
                               "sum(md.count) ASC LIMIT %(limit)s) derived ORDER BY RAND() LIMIT 1".format(
                                    message_type, ids, ids, ids
                                )

        else:
            return 0

        inner_limit = 2

        if len(words) // 4 > 2:
            inner_limit = len(words) // 4

        conn = self.db.get_connection()
        cursor = conn.cursor(dictionary=True)
        cursor.execute(select_statement, {'limit': inner_limit})

        result = cursor.fetchone()
        if result is not None:
            return result['second_id']

        return 0

    def _generate_from_seedword(self, message_type: str, seed_word: int):
        conn = self.db.get_connection()
        cursor = conn.cursor()

        min_length = 1
        sentence = ""

        if message_type == 'emote':
            procedure = 'generateMarkovEmote'
        elif message_type == 'say':
            procedure = 'generateMarkovSay'
        elif message_type == 'novel':
            procedure = 'generateMarkovNovel'
            min_length = 150
        else:
            return ''

        cur_words = 0

        while cur_words < min_length:
            result_args = cursor.callproc(procedure, [seed_word, 0])

            next_sentence: str = result_args[1]

            if next_sentence is None:
                seed_word = 0
                continue

            if seed_word != 0:
                first_word = markov_processor.get_markov_word_by_id(seed_word)
                if first_word is not None:
                    next_sentence = first_word + " " + next_sentence

            if len(next_sentence.split()) >= 5:
                seed_word = self._get_seed_word(message_type, next_sentence, seed_word)
            else:
                seed_word = 0

            if message_type != 'emote':
                next_sentence.capitalize()

            if sentence != '':
                next_sentence = " " + next_sentence

            if re.search(r'[.,?!:;/"\'-]+$', sentence):
                ending = '.'

                if random.randrange(100) < 35:
                    ending = random.choice(self.ending_punctuation)

                next_sentence = re.sub(r'[.,?!:;/"\'-]*$', ending, next_sentence)

            cur_words += len(next_sentence.split())
            sentence += next_sentence

            if random.randrange(100) < ((1 - ((min_length - cur_words) / min_length)) * 25):
                break

        return sentence
