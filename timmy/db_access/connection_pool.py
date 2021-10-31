import inspect
import logging
from typing import Dict, Optional

import mysql.connector
from mysql.connector import Error, pooling
from mysql.connector.pooling import MySQLConnectionPool, PooledMySQLConnection


class ConnectionPool:
    __pool: Optional[MySQLConnectionPool]

    __connection_count: Dict[str, int]

    def __init__(self):
        self.__pool = None
        self.__connection_count = {}

    def setup(self, host: str, database: str, user: str, password: str, port: int = 3306) -> None:
        try:
            self.__pool = mysql.connector.pooling.MySQLConnectionPool(
                    pool_name = "timmy_pool",
                    pool_size = 32,
                    pool_reset_session = True,
                    host = host,
                    database = database,
                    user = user,
                    password = password,
                    autocommit = True,
                    port = port
            )
        except Error as e:
            print("Error while connecting to database using connection pool: ", e)

    def get_connection(self) -> Optional[PooledMySQLConnection]:
        curframe = inspect.currentframe()
        calframe = inspect.getouterframes(curframe, 2)

        from timmy.utilities import irc_logger

        if calframe[1][3] in self.__connection_count:
            self.__connection_count[calframe[1][3]] += 1
        else:
            self.__connection_count[calframe[1][3]] = 1

        irc_logger.log_message(f"DB Con Request: {calframe[1][3]} Existing: {self.__connection_count[calframe[1][3]]}",
                               logging.DEBUG)
        connection = self.__pool.get_connection()

        if connection.is_connected():
            return connection
        else:
            self.__pool.reset_session()

    def close_connection(self, connection: PooledMySQLConnection) -> None:
        curframe = inspect.currentframe()
        calframe = inspect.getouterframes(curframe, 2)

        from timmy.utilities import irc_logger

        if calframe[1][3] in self.__connection_count:
            self.__connection_count[calframe[1][3]] -= 1
        else:
            irc_logger.log_message("WTF? Somehow closing a connection before we have one.")
            self.__connection_count[calframe[1][3]] = 0

        irc_logger.log_message(f"DB Closed: {calframe[1][3]} Remaining: {self.__connection_count[calframe[1][3]]}",
                               logging.DEBUG)

        connection.close()
