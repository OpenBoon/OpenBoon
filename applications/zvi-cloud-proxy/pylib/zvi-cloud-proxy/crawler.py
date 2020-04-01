import json
import os
import time

import yaml

from zmlp import ZmlpApp, FileUpload


class Crawler(object):
    def __init__(self):

        try:
            with open("../properties.yaml", 'r') as stream:
                try:
                    self.properties = yaml.safe_load(stream)
                    self.app = ZmlpApp(self.properties["api_key"], self.properties["zmlp_server"])
                except yaml.YAMLError as exc:
                    print("Failed on yaml reading: {0}".format(exc))
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


if __name__ == "__main__":
    Crawler()
