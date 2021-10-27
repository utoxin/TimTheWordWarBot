import inspect
import logging
from typing import Optional

import mysql.connector
from mysql.connector import Error, pooling
from mysql.connector.pooling import MySQLConnectionPool, PooledMySQLConnection


class ConnectionPool:
    __pool: Optional[MySQLConnectionPool]

    def __init__(self):
        self.__pool = None

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
        irc_logger.log_message(f"DB Connection Requested From: {calframe[1][3]}", logging.DEBUG)

        connection = self.__pool.get_connection()

        if connection.is_connected():
            return connection
        else:
            self.__pool.reset_session()