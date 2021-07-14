import mysql.connector
from mysql.connector import Error
from mysql.connector import pooling


class ConnectionPool:
    def __init__(self):
        self.__pool = None

    def setup(self, host, database, user, password, port=3306):
        try:
            self.__pool = mysql.connector.pooling.MySQLConnectionPool(
                    pool_name="timmy_pool",
                    pool_size=32,
                    pool_reset_session=True,
                    host=host,
                    database=database,
                    user=user,
                    password=password,
                    autocommit=True,
                    port=port
            )
        except Error as e:
            print("Error while connecting to database using connection pool: ", e)

    def get_connection(self):
        connection = self.__pool.get_connection()

        if connection.is_connected():
            return connection
