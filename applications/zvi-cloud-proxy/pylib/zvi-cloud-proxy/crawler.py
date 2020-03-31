import os
import time

import yaml


class Crawler(object):
    def __init__(self):

        try:
            with open("../properties.yaml", 'r') as stream:
                try:
                    self.properties = yaml.safe_load(stream)
                except yaml.YAMLError as exc:
                    print("Failed on yaml reading: {0}".format(exc))
        except FileNotFoundError as err:
            self.properties = {}
            print("yaml not found: {0}".format(str(err)))

    def crawl(self):

        batch = []
        uploaded = []

        while True:
            time.sleep(1)
            for mount in self.properties["mounts"]:
                for path, dirnames, names in os.walk(mount):
                    for name in names:
                        filename = "{0}{1}".format(path, name)
                        if filename not in batch and filename not in uploaded:
                            batch.append(filename)
                        if len(batch) > int(self.properties["batch_size"]):
                            try:
                                self.upload_batch(batch)
                                uploaded.extend(batch)
                                batch = []
                            except:
                                print("capture exception and try again")

    def upload_batch(self, batch):
        print(batch)
