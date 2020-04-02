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
                                    print(error)

    def upload_batch(self, batch):
        assets = [FileUpload(path) for path in batch]
        print(json.dumps(self.app.assets.batch_upload_files(assets)))

    def check_ext(self, filename):
        name, ext = os.path.splitext(filename)
        if not ext:
            return False
        if ext[1:] in self.properties["supported_types"]:
            return True
        return False


class SqliteUtils(object):

    def __init__(self, db_path):
        self.db_path = db_path
        self.con = sqlite3.connect(self.db_path)
        self.create_tables()

    def toHash(self, path):
        return hashlib.sha1(path.encode('utf-8')).hexdigest()

    def create_tables(self):

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
        sql = """ INSERT INTO assets  VALUES ('%s','%s','%s')""" % (self.toHash(path), path, datetime.datetime.now())

        try:
            cursor = self.con.cursor()
            cursor.execute(sql)
            self.con.commit()
        except Exception as e:
            raise ZmlpException(e)

    def insert_batch(self, batch):
        for file in batch:
            self.insert(file)


if __name__ == "__main__":
    Crawler()
