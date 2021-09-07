-- 
-- depends: 20210720_02_fJ1QX

CREATE TABLE `pychance_basic_tables`
(
    `uuid`       varchar(36)  NOT NULL,
    `table_name` varchar(255) NOT NULL
) DEFAULT CHARSET = utf8;

CREATE TABLE `pychance_basic_table_entries`
(
    `uuid`                    varchar(36)  NOT NULL,
    `pychance_basic_table_id` varchar(36)  NOT NULL,
    `table_entry`             varchar(255) NOT NULL
) DEFAULT CHARSET = utf8;

ALTER TABLE `pychance_basic_tables`
    ADD PRIMARY KEY (`uuid`),
    ADD KEY `table_name` (`table_name`);

ALTER TABLE `pychance_basic_table_entries`
    ADD PRIMARY KEY (`uuid`),
    ADD KEY `pychance_basic_table_id` (`pychance_basic_table_id`);
