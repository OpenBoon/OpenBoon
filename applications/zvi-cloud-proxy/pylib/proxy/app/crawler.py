import json
import os
import time
import yaml

from multiprocessing import Process
from zmlp import ZmlpApp, FileUpload
from zmlp.exception import ZmlpException
from proxy.app.sqlite_utils import SqliteUtils


class Crawler(object):
    """
    Crawler is used to walk through paths and uploads to ZMLP files
    filtered by extension and location folder
    """

    def __init__(self, properties_file_path="../properties.yaml", mount_path="/mnt"):

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
                    self.mount_path = mount_path
                except yaml.YAMLError as exc:
                    raise ZmlpException("Failed on yaml reading: {0}".format(exc))

        except FileNotFoundError as err:
            self.properties = {}
            raise ZmlpException(err, "Properties File not found")

    def run(self):

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
                folders = "%s/%s" % (self.mount_path, mount)
                for path, dirnames, names in os.walk(folders):
                    for name in names:
                        filename = "{0}/{1}".format(path, name)

                        if filename not in batch and not self.db_utils.exists(filename) and self.check_ext(filename):
                            print("enqueued ", filename)
                            batch.append(filename)
                            if len(batch) >= int(self.properties["batch_size"]):
                                try:
                                    # Start Async Upload
                                    process = Process(target=lambda: print(json.dumps(self.upload_batch(batch))))
                                    process.start()

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
        print("Uploading ", batch)
        assets = [FileUpload(path) for path in batch]
        return self.app.assets.batch_upload_files(assets)

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


if __name__ == "__main__":
    Crawler().run()
