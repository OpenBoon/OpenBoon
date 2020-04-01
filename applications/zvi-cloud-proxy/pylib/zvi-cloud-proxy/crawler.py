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
    def __init__(self):

        try:
            with open("../properties.yaml", 'r') as stream:
                try:

                    self.properties = yaml.safe_load(stream)
                    self.app = ZmlpApp(self.properties["api_key"], self.properties["zmlp_server"])
                    self.db_utils = SqliteUtils(self.properties["sqlite_db"])

                except yaml.YAMLError as exc:
                    raise ZmlpException("Failed on yaml reading: {0}".format(exc))

        except FileNotFoundError as err:
            self.properties = {}
            print("yaml not found: {0}".format(str(err)))

        self.crawl()

    def crawl(self):

        batch = []
        uploaded = []

        while True:
            time.sleep(1)
            for mount in self.properties["mounts"]:
                for path, dirnames, names in os.walk(mount):
                    for name in names:
                        filename = "{0}/{1}".format(path, name)
                        if filename not in batch and filename not in uploaded:
                            print(filename)
                            batch.append(filename)
                        if len(batch) >= int(self.properties["batch_size"]):
                            try:
                                self.upload_batch(batch)
                                uploaded.extend(batch)
                                batch = []
                            except ValueError as error:
                                print(error)

    def upload_batch(self, batch):
        assets = [FileUpload(path) for path in batch]
        print(json.dumps(self.app.assets.batch_upload_files(assets)))


class SqliteUtils(object):

    def __init__(self, path):
        self.path = path
        self.con = sqlite3.connect(path)
        self.create_tables()

    def toHash(self, path):
        return hashlib.sha1(path)

    def create_tables(self):

        sql = """
        CREATE TABLE IF NOT EXISTS assets (
            hash text NOT NULL,
            url text NOT NULL,
            date text
            PRIMARY KEY (hash)
        );
        """
        try:
            cursor = self.con.cursor()
            a = cursor.execute(sql)
        except Exception as e:
            raise ZmlpException(e)

    def exists(self, path):

        sql = """
        SELECT COUNT(*) FROM assets WHERE hash = ? 
        """

        try:
            cursor = self.con.cursor()
            cursor.execute(sql, self.toHash(path))
            result = cursor.fetchone()
            return int(result) == 1
        except Exception as e:
            raise ZmlpException(e)

    def insert(self, path):
        sql = """ INSERT INTO assets  VALUES (?,?,?)"""

        try:
            cursor = self.con.cursor()
            cursor.execute(sql, self.toHash(path), path, datetime.datetime.now())
        except Exception as e:
            raise ZmlpException(e)


if __name__ == "__main__":
    Crawler()
