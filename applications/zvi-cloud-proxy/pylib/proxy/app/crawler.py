import json
import os
import time
from zmlp.exception import ZmlpException
import sqlite3
import yaml
import hashlib
import datetime

from zmlp import ZmlpApp, FileUpload


class Crawler(object):
    """
    Crawler is used to walk through paths and uploads to ZMLP files
    filtered by extension and location folder
    """

    def __init__(self, properties_file_path="../properties.yaml"):

        """
            Initialize application, Load properties from properties.yaml
            and database
        """

        try:
            with open(properties_file_path, 'r') as stream:
                try:
                    self.properties = yaml.safe_load(stream)
                    self.app = ZmlpApp(self.properties["api_key"], self.properties["zmlp_server"])
                    self.db_utils = SqliteUtils(self.properties["sqlite_db"])
                except yaml.YAMLError as exc:
                    raise ZmlpException("Failed on yaml reading: {0}".format(exc))

        except FileNotFoundError as err:
            self.properties = {}
            raise ZmlpException(err, "Properties File not found")
        self.crawl()

    def crawl(self):

        """
        - Loop with 1 second interval
        - Searches and Uploads files by mounted folders and extension type
        - References of uploaded files are stored in a local database
        - batch list keep files that has not been uploaded yet, when
        it reach it's maximum defined size, the batch is uploaded and
        references are stored in local database

        :return:
        """

        batch = []

        while True:
            time.sleep(1)
            for mount in self.properties["mounts"]:
                folders = "/mnt/%s" % mount
                for path, dirnames, names in os.walk(folders):
                    for name in names:
                        filename = "{0}/{1}".format(path, name)

                        if filename not in batch and not self.db_utils.exists(filename) and self.check_ext(filename):
                            print("enqueued ", filename)
                            batch.append(filename)

                            if len(batch) >= int(self.properties["batch_size"]):
                                try:
                                    print("upload batch ", batch)
                                    self.upload_batch(batch)
                                    self.db_utils.insert_batch(batch)
                                    batch = []
                                except ValueError as error:
                                    raise ZmlpException(error)

    def upload_batch(self, batch):
        """
        Upload files
        :param batch: List of paths to be uploaded
        :return:
        """
        assets = [FileUpload(path) for path in batch]
        print(json.dumps(self.app.assets.batch_upload_files(assets)))

    def check_ext(self, filename):

        """
        Return if file extension is in supported_types field on properties.yml
        :param filename: Filename to be evaluated
        :return: True if extension is in supported_types field
        """

        name, ext = os.path.splitext(filename)
        if not ext:
            return False
        if ext[1:] in self.properties["supported_types"]:
            return True
        return False


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
            a = cursor.execute(sql)
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


if __name__ == "__main__":
    Crawler()
