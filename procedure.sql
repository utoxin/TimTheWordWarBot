DROP PROCEDURE IF EXISTS `generateMarkovSay`;
DROP PROCEDURE IF EXISTS `generateMarkovEmote`;

DELIMITER $$
CREATE PROCEDURE `generateMarkovSay`(IN seedWord INT(10), OUT finalSentence TEXT)
  BEGIN
    DECLARE sentence TEXT;
    DECLARE lastWord VARCHAR(100);

    DECLARE word1, word2, word3, total, pick, junk INT(10) UNSIGNED DEFAULT 1;

    DECLARE countCurs CURSOR FOR SELECT SUM(count) AS total
                                 FROM markov3_emote_data
                                 WHERE first_id = word1 AND second_id = word2;

    DECLARE fetchCurs CURSOR FOR SELECT
                                   markov_words.id,
                                   markov_words.word,
                                   rt
                                 FROM (SELECT
                                         first_id,
                                         second_id,
                                         third_id,
                                         (@runtot := @runtot + count) AS rt
                                       FROM (SELECT *
                                             FROM `markov3_emote_data`
                                             WHERE first_id = word1 AND second_id = word2) derived,
                                         (SELECT @runtot := 0) r) derived2 INNER JOIN markov_words
                                     ON (derived2.third_id = markov_words.id)
                                 HAVING rt > pick
                                 LIMIT 1;

    DECLARE CONTINUE HANDLER FOR NOT FOUND BEGIN
      SET word3 = 1;
      SET lastWord = NULL;
    END;

    SET word3 = 2;

    IF (seedWord > 0)
    THEN
      SET word2 = seedWord;
    END IF;

    WHILE word3 > 1 DO
      OPEN countCurs;
      FETCH countCurs
      INTO total;
      CLOSE countCurs;

      IF total > 0
      THEN
        SET pick = FLOOR(RAND() * total);

        OPEN fetchCurs;
        FETCH fetchCurs
        INTO word3, lastWord, junk;
        CLOSE fetchCurs;

        IF word3 > 1
        THEN
          IF lastWord IS NOT NULL
          THEN
            IF sentence IS NULL
            THEN
              SET sentence = lastWord;
            ELSE
              SET sentence = CONCAT(sentence, ' ', lastWord);
            END IF;
          END IF;

          SET word1 = word2;
          SET word2 = word3;
        END IF;
      ELSE
        SET word1 = word2;
        SET word2 = 1;
      END IF;
    END WHILE;

    SET finalSentence = sentence;
  END$$

CREATE PROCEDURE `generateMarkovEmote`(IN seedWord INT(10), OUT finalSentence TEXT)
  BEGIN
    DECLARE sentence TEXT;
    DECLARE lastWord VARCHAR(100);

    DECLARE word1, word2, word3, total, pick, junk INT(10) UNSIGNED DEFAULT 1;

    DECLARE countCurs CURSOR FOR SELECT SUM(count) AS total
                                 FROM markov3_emote_data
                                 WHERE first_id = word1 AND second_id = word2;
    DECLARE fetchCurs CURSOR FOR SELECT
                                   markov_words.id,
                                   markov_words.word,
                                   rt
                                 FROM (SELECT
                                         first_id,
                                         second_id,
                                         third_id,
                                         (@runtot := @runtot + count) AS rt
                                       FROM (SELECT *
                                             FROM `markov3_emote_data`
                                             WHERE first_id = word1 AND second_id = word2) derived,
                                         (SELECT @runtot := 0) r) derived2 INNER JOIN markov_words
                                     ON (derived2.third_id = markov_words.id)
                                 HAVING rt > pick
                                 LIMIT 1;

    DECLARE CONTINUE HANDLER FOR NOT FOUND BEGIN
      SET word3 = 1;
      SET lastWord = NULL;
    END;

    SET word3 = 2;

    IF (seedWord > 0)
    THEN
      SET word2 = seedWord;
    END IF;

    WHILE word3 > 1 DO
      OPEN countCurs;
      FETCH countCurs
      INTO total;
      CLOSE countCurs;

      IF total > 0
      THEN
        SET pick = FLOOR(RAND() * total);

        OPEN fetchCurs;
        FETCH fetchCurs
        INTO word3, lastWord, junk;
        CLOSE fetchCurs;

        IF word3 > 1
        THEN
          IF lastWord IS NOT NULL
          THEN
            IF sentence IS NULL
            THEN
              SET sentence = lastWord;
            ELSE
              SET sentence = CONCAT(sentence, ' ', lastWord);
            END IF;
          END IF;

          SET word1 = word2;
          SET word2 = word3;
        END IF;
      ELSE
        SET word1 = word2;
        SET word2 = 1;
      END IF;
    END WHILE;

    SET finalSentence = sentence;
  END$$
