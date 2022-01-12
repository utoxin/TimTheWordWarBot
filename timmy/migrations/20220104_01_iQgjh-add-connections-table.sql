-- Add Connections Table
-- depends: 20211105_01_nwp9G-set-charset-for-markov-tables

create table connections
(
    connection_tag char(36)     not null,
    module         varchar(255) not null,
    config         blob         not null,
    constraint connections_pk
        primary key (connection_tag)
);