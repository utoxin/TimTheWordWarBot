import logging
import random
import re
import sys
import traceback
from typing import Dict, List, Optional, Pattern, Set, Tuple, Union

import schedule

from timmy import db_access


class MarkovProcessor:
    bad_words: Set[Pattern[str]]
    bad_pairs: Set[Tuple[Pattern[str], Pattern[str]]]
    alternate_words: Set[str]
    alternate_pairs: Set[Tuple[str, str]]

    def __init__(self):
        self.db = None
        self.bad_words = set()
        self.bad_pairs = set()
        self.alternate_words = set()
        self.alternate_pairs = set()

        self.clean_string_pattern = re.compile('[^a-z]', re.IGNORECASE)
        self.all_upper_pattern = re.compile('^[A-Z]+$')
        self.starts_upper_pattern = re.compile('^[A-Z]+[a-z]+$')

    def init(self) -> None:
        if self.db is None:
            self.db = db_access.connection_pool

            self._load_bad_words()
            self._load_bad_pairs()
            self._load_alternate_words()
            self._load_alternate_pairs()

            schedule.every(1).minutes.do(markov_processing_loop).tag('markovticker')

    def reset_timer(self) -> None:
        schedule.clear('markovticker')

        self.__init__()
        self.init()

    def store_line(self, line_type: str, line: str) -> None:
        self.init()

        conn = self.db.get_connection()
        insert_statement = "INSERT INTO markov_processing_queue (`type`, `text`, `created`) VALUES (%(line_type)s, " \
                           "%(line)s, NOW())"

        cursor = conn.cursor()
        cursor.execute(
                insert_statement, {
                    'line_type': line_type,
                    'line':      line
                }
        )

        self.db.close_connection(conn)

    def processing_loop(self) -> None:
        self.init()

        select_statement = "SELECT `id`, `type`, `text` FROM markov_processing_queue"
        delete_statement = "DELETE FROM `markov_processing_queue` WHERE `id` = %(id)s"

        select_conn = self.db.get_connection()
        select_cursor = select_conn.cursor(dictionary = True)

        delete_conn = self.db.get_connection()
        delete_cursor = delete_conn.cursor()

        select_cursor.execute(select_statement)
        for row in select_cursor:
            if row['type'] == 'emote' or row['type'] == 'say':
                self._process_markov(row['text'], row['type'])
            elif row['type'] == 'novel':
                self._process_markov4(row['text'], row['type'])
                self._process_markov(row['text'], 'say')

            delete_cursor.execute(delete_statement, {'id': row['id']})

        self.db.close_connection(select_conn)
        self.db.close_connection(delete_conn)


    def _process_markov(self, message: str, message_type: str) -> None:
        known_replacements: Dict[Union[Tuple[str, str], str], Union[Tuple[str, str], str]] = {}
        full_message = message.split()

        for i in range(-1, len(full_message) + 1):
            words = [
                [i - 1, ""],
                [i, ""],
                [i + 1, ""]
            ]

            self._internal_markov_processing(full_message, known_replacements, words)
            self._store_triad(words[0][1], words[1][1], words[2][1], message_type)

    def _internal_markov_processing(
            self, full_message: List[str],
            known_replacements: Dict[Union[Tuple[str, str], str], Union[Tuple[str, str], str]], words: list
    ) -> None:
        for word in words:
            word[1] = self.__set_word(word[0], full_message)
            word[1] = self.__replace_word(known_replacements, word[1])

        if len(full_message) > 1:
            for j, k in zip(range(len(words)), range(len(words))[1:]):
                words[j][1], words[k][1] = self.__replace_pair(known_replacements, words[j][1], words[k][1])

        for word in words:
            word[1] = word[1][:50]
            if 0 <= word[0] < len(full_message):
                full_message[word[0]] = word[1]

    def _process_markov4(self, message: str, message_type: str) -> None:
        known_replacements = {}
        full_message = message.split()

        for i in range(-1, len(full_message) + 1):
            words = [
                [i - 2, ""],
                [i - 1, ""],
                [i, ""],
                [i + 1, ""]
            ]

            self._internal_markov_processing(full_message, known_replacements, words)
            self._store_quad(words[0][1], words[1][1], words[2][1], words[3][1], message_type)

    def __replace_pair(
            self, known_replacements: Dict[Union[Tuple[str, str], str], Union[Tuple[str, str], str]], word1: str,
            word2: str
    ) -> Tuple[str, str]:
        pair = (word1, word2)
        pair_string = "{} {}".format(word1, word2)

        if pair_string in known_replacements:
            word1, word2 = known_replacements[pair_string].split()
        else:
            rep = self._replace_bad_pair(pair)

            if rep != pair:
                known_replacements[pair_string] = rep
                word1, word2 = rep

        return word1, word2

    def __replace_word(
            self, known_replacements: Dict[Union[Tuple[str, str], str], Union[Tuple[str, str], str]], word: str
    ) -> str:
        if word in known_replacements:
            word = known_replacements[word]
        else:
            rep = self._replace_bad_word(word)
            known_replacements[word] = rep
            word = rep

        return word

    @staticmethod
    def __set_word(offset1: int, words: List[str]) -> str:
        if offset1 < 0:
            word1 = ""
        elif offset1 >= len(words):
            word1 = ""
        else:
            word1 = words[offset1]
        return word1

    def _store_triad(self, word1: str, word2: str, word3: str, message_type: str) -> None:
        first = self.get_markov_word_id(word1)
        second = self.get_markov_word_id(word2)
        third = self.get_markov_word_id(word3)

        conn = self.db.get_connection()

        if message_type != 'emote':
            message_type = 'say'

        add_triad_expression = f"INSERT INTO `markov3_{message_type}_data` (`first_id`, `second_id`, `third_id`, " \
                               f"`count`) VALUES (%(first)s, %(second)s, %(third)s, 1) ON DUPLICATE KEY UPDATE " \
                               f"count = count + 1"

        cursor = conn.cursor()
        cursor.execute(
                add_triad_expression, {
                    'first':  first,
                    'second': second,
                    'third':  third
                }
        )

        self.db.close_connection(conn)

    def _store_quad(self, word1: str, word2: str, word3: str, word4: str, message_type: str) -> None:
        first = self.get_markov_word_id(word1)
        second = self.get_markov_word_id(word2)
        third = self.get_markov_word_id(word3)
        fourth = self.get_markov_word_id(word4)

        conn = self.db.get_connection()

        if message_type == 'novel':
            return

        add_quad_expression = f"INSERT INTO `markov4_{message_type}_data` (`first_id`, `second_id`, `third_id`, " \
                              f"`fourth_id`, `count`) VALUES (%(first)s, %(second)s, %(third)s, %(fourth)s, 1) ON " \
                              f"DUPLICATE KEY UPDATE count = count + 1"

        cursor = conn.cursor()
        cursor.execute(
                add_quad_expression, {
                    'first':  first,
                    'second': second,
                    'third':  third,
                    'fourth': fourth
                }
        )

        self.db.close_connection(conn)

    def get_markov_word_id(self, word: str) -> int:
        self.init()

        conn = self.db.get_connection()

        word = word[:50]

        select_word_statement = "SELECT id FROM markov_words WHERE word = %(word)s"
        add_word_statement = "INSERT INTO markov_words SET word = %(word)s"

        cursor = conn.cursor(dictionary = True)
        cursor.execute(select_word_statement, {'word': word})

        result = cursor.fetchone()

        if result is not None:
            self.db.close_connection(conn)

            return result['id']
        else:
            cursor.execute(add_word_statement, {'word': word})

            self.db.close_connection(conn)

            return cursor.lastrowid

    def get_markov_word_by_id(self, word_id: int) -> Optional[str]:
        self.init()

        conn = self.db.get_connection()

        select_statement = "SELECT word FROM markov_words WHERE id = %(id)s"

        cursor = conn.cursor(dictionary = True)
        cursor.execute(select_statement, {'id': word_id})

        result = cursor.fetchone()

        self.db.close_connection(conn)

        if result is not None:
            return result['word'].decode('utf-8')
        else:
            return None

    def add_bad_word(self, word: str) -> None:
        insert_sql = "REPLACE INTO bad_words SET word = %(bad_word)s"
        delete_sql = "DELETE FROM markov_words WHERE word COLLATE utf8_general_ci REGEXP %(regexp)s"

        conn = self.db.get_connection()
        cursor = conn.cursor()

        cursor.execute(insert_sql, {'bad_word': word})
        cursor.execute(delete_sql, {'regexp': f"^[[:punct:]]*{word}[[:punct:]]*$"})

        self.db.close_connection(conn)

        self.bad_words.add(re.compile('(\\W|\\b)({})(\\W|\\b)'.format(re.escape(word)), re.IGNORECASE))

    def add_bad_pair(self, word_one: str, word_two: str) -> None:
        insert_sql = "REPLACE INTO bad_pairs SET word_one = %(word_one)s, word_two = %(word_two)s"

        delete_sql_1 = "DELETE msd.* FROM markov3_say_data msd INNER JOIN markov_words mw1 ON (msd.first_id = mw1.id)" \
                       " INNER JOIN markov_words mw2 ON (msd.second_id = mw2.id) WHERE mw1.word " \
                       "COLLATE utf8_general_ci REGEXP %(word_one)s AND mw2.word " \
                       "COLLATE utf8_general_ci REGEXP %(word_two)s"

        delete_sql_2 = "DELETE msd.* FROM markov3_say_data msd INNER JOIN markov_words mw1 ON (msd.second_id = " \
                       "mw1.id) INNER JOIN markov_words mw2 ON (msd.third_id = mw2.id) WHERE mw1.word " \
                       "COLLATE utf8_general_ci REGEXP %(word_one)s AND mw2.word " \
                       "COLLATE utf8_general_ci REGEXP %(word_two)s"

        delete_sql_3 = "DELETE msd.* FROM markov3_emote_data msd INNER JOIN markov_words mw1 ON (msd.first_id = " \
                       "mw1.id) INNER JOIN markov_words mw2 ON (msd.second_id = mw2.id) WHERE mw1.word " \
                       "COLLATE utf8_general_ci REGEXP %(word_one)s AND mw2.word " \
                       "COLLATE utf8_general_ci REGEXP %(word_two)s"

        delete_sql_4 = "DELETE msd.* FROM markov3_emote_data msd INNER JOIN markov_words mw1 ON (msd.second_id = " \
                       "mw1.id) INNER JOIN markov_words mw2 ON (msd.third_id = mw2.id) WHERE mw1.word " \
                       "COLLATE utf8_general_ci REGEXP %(word_one)s AND mw2.word " \
                       "COLLATE utf8_general_ci REGEXP %(word_two)s"

        delete_sql_5 = "DELETE msd.* FROM markov3_emote_data msd INNER JOIN markov_words mw1 ON (msd.first_id = " \
                       "mw1.id) INNER JOIN markov_words mw2 ON (msd.second_id = mw2.id) WHERE mw1.word " \
                       "COLLATE utf8_general_ci REGEXP %(word_one)s AND mw2.word " \
                       "COLLATE utf8_general_ci REGEXP %(word_two)s"

        delete_sql_6 = "DELETE msd.* FROM markov3_emote_data msd INNER JOIN markov_words mw1 ON (msd.second_id = " \
                       "mw1.id) INNER JOIN markov_words mw2 ON (msd.third_id = mw2.id) WHERE mw1.word " \
                       "COLLATE utf8_general_ci REGEXP %(word_one)s AND mw2.word " \
                       "COLLATE utf8_general_ci REGEXP %(word_two)s"

        delete_sql_7 = "DELETE msd.* FROM markov3_emote_data msd INNER JOIN markov_words mw1 ON (msd.third_id = " \
                       "mw1.id) INNER JOIN markov_words mw2 ON (msd.fourth_id = mw2.id) WHERE mw1.word " \
                       "COLLATE utf8_general_ci REGEXP %(word_one)s AND mw2.word " \
                       "COLLATE utf8_general_ci REGEXP %(word_two)s"

        conn = self.db.get_connection()
        cursor = conn.cursor()

        cursor.execute(insert_sql, {'word_one': word_one, 'word_two': word_two})

        cursor.execute(delete_sql_1, {
            'word_one': f"^[[:punct:]]*{word_one}[[:punct:]]*$",
            'word_two': f"^[[:punct:]]*{word_two}[[:punct:]]*$"
        })

        cursor.execute(delete_sql_2, {
            'word_one': f"^[[:punct:]]*{word_one}[[:punct:]]*$",
            'word_two': f"^[[:punct:]]*{word_two}[[:punct:]]*$"
        })

        cursor.execute(delete_sql_3, {
            'word_one': f"^[[:punct:]]*{word_one}[[:punct:]]*$",
            'word_two': f"^[[:punct:]]*{word_two}[[:punct:]]*$"
        })

        cursor.execute(delete_sql_4, {
            'word_one': f"^[[:punct:]]*{word_one}[[:punct:]]*$",
            'word_two': f"^[[:punct:]]*{word_two}[[:punct:]]*$"
        })

        cursor.execute(delete_sql_5, {
            'word_one': f"^[[:punct:]]*{word_one}[[:punct:]]*$",
            'word_two': f"^[[:punct:]]*{word_two}[[:punct:]]*$"
        })

        cursor.execute(delete_sql_6, {
            'word_one': f"^[[:punct:]]*{word_one}[[:punct:]]*$",
            'word_two': f"^[[:punct:]]*{word_two}[[:punct:]]*$"
        })

        cursor.execute(delete_sql_7, {
            'word_one': f"^[[:punct:]]*{word_one}[[:punct:]]*$",
            'word_two': f"^[[:punct:]]*{word_two}[[:punct:]]*$"
        })

        self.db.close_connection(conn)

        self.bad_pairs.add(
                (
                    re.compile('(\\W|\\b)({})(\\W|\\b)'.format(re.escape(word_one)), re.IGNORECASE),
                    re.compile('(\\W|\\b)({})(\\W|\\b)'.format(re.escape(word_two)), re.IGNORECASE),
                )
        )

    def _load_bad_words(self) -> None:
        conn = self.db.get_connection()
        select_statement = "SELECT `word` FROM `bad_words`"

        cursor = conn.cursor(dictionary = True)
        cursor.execute(select_statement)

        for row in cursor:
            self.bad_words.add(re.compile('(\\W|\\b)({})(\\W|\\b)'.format(re.escape(row['word'])), re.IGNORECASE))

        self.db.close_connection(conn)

    def _load_bad_pairs(self) -> None:
        conn = self.db.get_connection()
        select_statement = "SELECT `word_one`, `word_two` FROM `bad_pairs`"

        cursor = conn.cursor(dictionary = True)
        cursor.execute(select_statement)

        for row in cursor:
            self.bad_pairs.add(
                    (
                        re.compile('(\\W|\\b)({})(\\W|\\b)'.format(re.escape(row['word_one'])), re.IGNORECASE),
                        re.compile('(\\W|\\b)({})(\\W|\\b)'.format(re.escape(row['word_two'])), re.IGNORECASE),
                    )
            )

        self.db.close_connection(conn)

    def _load_alternate_words(self) -> None:
        conn = self.db.get_connection()
        select_statement = "SELECT `word` FROM `alternate_words`"

        cursor = conn.cursor(dictionary = True)
        cursor.execute(select_statement)

        for row in cursor:
            self.alternate_words.add(row['word'])

        self.db.close_connection(conn)

    def _load_alternate_pairs(self) -> None:
        conn = self.db.get_connection()
        select_statement = "SELECT `word_one`, `word_two` FROM `alternate_pairs`"

        cursor = conn.cursor(dictionary = True)
        cursor.execute(select_statement)

        for row in cursor:
            self.alternate_pairs.add((row['word_one'], row['word_two']))

        self.db.close_connection(conn)

    def _replace_bad_word(self, word: str) -> str:
        if len(self.bad_words) == 0:
            return word

        old_word = word
        replacement: str = random.choice(tuple(self.alternate_words))
        working = self.clean_string_pattern.sub('', word)

        if self.all_upper_pattern.match(working):
            replacement = replacement.upper()
        elif self.starts_upper_pattern.match(working):
            replacement = replacement.capitalize()

        for bad_word_pattern in self.bad_words:
            word = bad_word_pattern.sub(r'\1{}\3'.format(replacement), word)

            if word != old_word:
                break

        if self._is_url(word) or self._is_valid_email(word):
            word = 'https://amzn.to/3BwoMyx'

        if re.match("^\\(?(\\d{3})\\)?[- ]?(\\d{2,3})[- ]?(\\d{4})$", word):
            word = '867-5309'

        return word

    def _replace_bad_pair(self, pair: Tuple[str, str]) -> Tuple[str, str]:
        if len(self.bad_pairs):
            return pair

        alternate1, alternate2 = random.choice(tuple(self.alternate_pairs))

        working1 = self.clean_string_pattern.sub('', pair[0])
        working2 = self.clean_string_pattern.sub('', pair[1])

        if self.all_upper_pattern.match(working1):
            alternate1 = alternate1.upper()
        elif self.starts_upper_pattern.match(working1):
            alternate1 = alternate1.capitalize()

        if self.all_upper_pattern.match(working2):
            alternate2 = alternate2.upper()
        elif self.starts_upper_pattern.match(working1):
            alternate2 = alternate2.capitalize()

        for bad_pattern_pair in self.bad_pairs:
            if bad_pattern_pair[0].match(pair[0]) is not None and bad_pattern_pair[1].match(pair[1]) is not None:
                new_pair1 = bad_pattern_pair[0].sub(r'\1{}\3'.format(alternate1), pair[0])
                new_pair2 = bad_pattern_pair[1].sub(r'\1{}\3'.format(alternate2), pair[1])

                return new_pair1, new_pair2

        return pair

    @staticmethod
    def _is_valid_email(email: str) -> bool:
        pattern = re.compile(r"^([a-z]|[0-9]|\-|\_|\+|\.)+\@([a-z]|[0-9]){2,}\.[a-z]{2,}(\.[a-z]{2,})?$",
                             re.IGNORECASE | re.MULTILINE | re.UNICODE)

        return re.match(pattern, email) is not None

    @staticmethod
    def _is_url(url: str) -> bool:
        return re.match(r'http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*(), ]|%[0-9a-fA-F][0-9a-fA-F])+', url) \
               is not None


def markov_processing_loop() -> None:
    try:
        from timmy.utilities import markov_processor
        markov_processor.processing_loop()
    except Exception:
        from timmy.utilities import irc_logger
        irc_logger.log_traceback()

