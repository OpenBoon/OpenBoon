import sqlite3
import hashlib
import datetime
from zmlp.exception import ZmlpException


class SqliteUtils(object):
    """
    Used to Sqlite database operations
    """

    def __init__(self, db_path):
        self.db_path = db_path
        self.con = sqlite3.connect(self.db_path)
        self.create_tables()

    def toHash(self, path):
        """
        Hash path string using sha1 algorithm
        :param path:
        :return:
        """
        return hashlib.sha1(path.encode('utf-8')).hexdigest()

    def create_tables(self):
        """
        Create local db tables, if not exists.
        :return:
        """
        sql = """
        CREATE TABLE IF NOT EXISTS assets (
            hash text PRIMARY_KEY,
            url text NOT NULL,
            date text
        );
        """
        try:
            cursor = self.con.cursor()
            cursor.execute(sql)
        except Exception as e:
            raise ZmlpException(e)

    def exists(self, path):
        """
        Verify if a file has already been uploaded.
        :param path: file path String
        :return: True if file has been uploaded.
        """

        sql = """
        SELECT COUNT(*) FROM assets WHERE hash = '%s' 
        """ % self.toHash(path)

        try:
            cursor = self.con.cursor()
            cursor.execute(sql)
            result, = cursor.fetchone()
            return int(result) == 1
        except Exception as e:
            raise ZmlpException(e)

    def insert(self, path):

        """
        Add File registry to 'assets' table
        :param path: file path
        :return:
        """

        sql = """ INSERT INTO assets  VALUES ('%s','%s','%s')""" % (self.toHash(path), path, datetime.datetime.now())

        try:
            cursor = self.con.cursor()
            cursor.execute(sql)
            self.con.commit()
        except Exception as e:
            raise ZmlpException(e)

    def insert_batch(self, batch):
        """
        Insert File batch in local folder
        :param batch: List of file paths
        :return:
        """
        for file in batch:
            self.insert(file)
