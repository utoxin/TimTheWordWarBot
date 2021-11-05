-- Set Charset for Markov Tables
-- depends: 20211012_01_ED6Ei-add-tz-column-for-channels

SET NAMES utf8mb4;
ALTER TABLE `markov_processing_queue` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `markov_words` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
