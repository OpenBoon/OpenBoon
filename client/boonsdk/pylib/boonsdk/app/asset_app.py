import os
import requests
from collections import namedtuple

from ..entity import Asset, StoredFile, FileUpload, FileTypes, Job, VideoClip
from ..search import AssetSearchResult, AssetSearchScroller, SimilarityQuery, SearchScroller
from ..util import as_collection, as_id_collection, as_id


class AssetApp(object):

    def __init__(self, app):
        self.app = app

    def batch_import_files(self, files, modules=None):
        """
        Import a list of FileImport instances.

        Args:
            files (list of FileImport): The list of files to import as Assets.
            modules (list): A list of Pipeline Modules to apply to the data.

        Notes:
            Example return value:
                {
                  "bulkResponse" : {
                    "took" : 15,
                    "errors" : false,
                    "items" : [ {
                      "create" : {
                        "_index" : "yvqg1901zmu5bw9q",
                        "_type" : "_doc",
                        "_id" : "dd0KZtqyec48n1q1fniqVMV5yllhRRGx",
                        "_version" : 1,
                        "result" : "created",
                        "forced_refresh" : true,
                        "_shards" : {
                          "total" : 1,
                          "successful" : 1,
                          "failed" : 0
                        },
                        "_seq_no" : 0,
                        "_primary_term" : 1,
                        "status" : 201
                      }
                    } ]
                  },
                  "failed" : [ ],
                  "created" : [ "dd0KZtqyec48n1q1fniqVMV5yllhRRGx" ],
                  "jobId" : "ba310246-1f87-1ece-b67c-be3f79a80d11"
                }

        Returns:
            dict: A dictionary containing an ES bulk response, failed files,
            and created asset ids.

        """
        body = {
            "assets": files,
            "modules": modules
        }
        return self.app.client.post("/api/v3/assets/_batch_create", body)

    def batch_upload_files(self, files, modules=None):
        """
        Batch upload a list of files and return a structure which contains
        an ES bulk response object, a list of failed file paths, a list of created
        asset Ids, and a processing jobId.

        Args:
            files (list of FileUpload):
            modules (list): A list of Pipeline Modules to apply to the data.

        Notes:
            Example return value:
                {
                  "bulkResponse" : {
                    "took" : 15,
                    "errors" : false,
                    "items" : [ {
                      "create" : {
                        "_index" : "yvqg1901zmu5bw9q",
                        "_type" : "_doc",
                        "_id" : "dd0KZtqyec48n1q1fniqVMV5yllhRRGx",
                        "_version" : 1,
                        "result" : "created",
                        "forced_refresh" : true,
                        "_shards" : {
                          "total" : 1,
                          "successful" : 1,
                          "failed" : 0
                        },
                        "_seq_no" : 0,
                        "_primary_term" : 1,
                        "status" : 201
                      }
                    } ]
                  },
                  "failed" : [ ],
                  "created" : [ "dd0KZtqyec48n1q1fniqVMV5yllhRRGx" ],
                  "jobId" : "ba310246-1f87-1ece-b67c-be3f79a80d11"
                }

        Returns:
            dict: A dictionary containing an ES bulk response, failed files,
            and created asset ids.
        """
        files = as_collection(files)
        file_paths = [f.uri for f in files]
        body = {
            "assets": files,
            "modules": modules
        }
        return self.app.client.upload_files("/api/v3/assets/_batch_upload",
                                            file_paths, body)

    def batch_upload_directory(self, path, file_types=None,
                               batch_size=50, modules=None, callback=None):
        """
        Recursively upload all files in the given directory path.

        This method takes an optional callback function which takes two
        arguments, files and response.  This callback is called for
        each batch of files submitted.

        Examples:

            def batch_callback(files, response):
                print("--processed files--")
                for path in files:
                    print(path)
                print("--boonsdk response--")
                pprint.pprint(rsp)

            app.assets.batch_upload_directory("/home", file_types=['images'],
                callback=batch_callback)

        Args:
            path (str): A file path to a directory.
            file_types (list): a list of file extensions and/or
                categories(documents, images, videos)
            batch_size (int) The number of files to upload per batch.
            modules (list): An array of modules to apply to the files.
            callback (func): A function to call for every batch

        Returns:
            dict: A dictionary containing batch operation counters.
        """
        batch = []
        totals = {
            "file_count": 0,
            "file_size": 0,
            "batch_count": 0,
        }

        def process_batch():
            totals['batch_count'] += 1
            totals['file_count'] += len(batch)
            totals['file_size'] += sum([os.path.getsize(f) for f in batch])

            rsp = self.batch_upload_files(
                [FileUpload(f) for f in batch], modules)
            if callback:
                callback(batch.copy(), rsp)
            batch.clear()

        file_types = FileTypes.resolve(file_types)
        for root, dirs, files in os.walk(path):
            for fname in files:
                if fname.startswith("."):
                    continue
                _, ext = os.path.splitext(fname)
                if not ext:
                    continue
                if ext[1:].lower() not in file_types:
                    continue
                batch.append(os.path.abspath(os.path.join(root, fname)))
                if len(batch) >= batch_size:
                    process_batch()

        if batch:
            process_batch()

        return totals

    def delete_asset(self, asset):
        """
        Delete the given asset.

        Args:
            asset (mixed): unique Id or Asset instance.

        Returns:
            bool: True if the asset was deleted.

        """
        asset_id = as_id(asset)
        return self.app.client.delete("/api/v3/assets/{}".format(asset_id))['success']

    def batch_delete_assets(self, assets):
        """
        Batch delete the given list of Assets or asset ids.

        Args:
            assets (list): A list of Assets or unique asset ids.

        Returns:
            dict: A dictionary containing deleted and errored asset Ids.
        """
        body = {
            "assetIds": as_id_collection(assets)
        }
        return self.app.client.delete("/api/v3/assets/_batch_delete", body)

    def search(self, search=None, fetch_source=True):
        """
        Perform an asset search using the ElasticSearch query DSL.

        See Also:
            For search/query format.
            https://www.elastic.co/guide/en/elasticsearch/reference/6.4/search-request-body.html

        Args:
            search (dict): The ElasticSearch search to execute.
            fetch_source: (bool): If true, the full JSON document for each asset is returned.
        Returns:
            AssetSearchResult - an AssetSearchResult instance.
        """
        if not fetch_source:
            search['_source'] = False
        return AssetSearchResult(self.app, search)

    def scroll_search(self, search=None, timeout="1m"):
        """
        Perform an asset scrolled search using the ElasticSearch query DSL.

        See Also:
            For search/query format.
            https://www.elastic.co/guide/en/elasticsearch/reference/6.4/search-request-body.html

        Args:
            search (dict): The ElasticSearch search to execute
            timeout (str): The scroll timeout.  Defaults to 1 minute.
        Returns:
            AssetSearchScroll - an AssetSearchScroller instance which is a generator
                by nature.

        """
        return AssetSearchScroller(self.app, search, timeout)

    def reprocess_search(self, search, modules):
        """
        Reprocess the given search with the supplied modules.

        Args:
            search (dict): An ElasticSearch search.
            modules (list): A list of module names to apply.

        Returns:
            dict: Contains a Job and the number of assets to be processed.
        """
        body = {
            "search": search,
            "modules": modules
        }
        rsp = self.app.client.post("/api/v3/assets/_search/reprocess", body)
        return ReprocessSearchResponse(rsp["assetCount"], Job(rsp["job"]))

    def scroll_search_clips(self, asset, search=None, timeout="1m"):
        """
        Scroll through clips for given asset using the ElasticSearch query DSL.

        Args:
            asset (Asset): The asset or unique AssetId.
            search (dict): The ElasticSearch search to execute
            timeout (str): The scroll timeout.  Defaults to 1 minute.

        Returns:
            SearchScroller  a clip scroller instance for generating VideoClips.

        """
        asset_id = as_id(asset)
        return SearchScroller(
            VideoClip, f'/api/v3/assets/{asset_id}/clips/_search', self.app, search, timeout
        )

    def reprocess_assets(self, assets, modules):
        """
        Reprocess the given array of assets with the given modules.

        Args:
            assets (list): A list of Assets or asset unique Ids.
            modules (list): A list of Pipeline module names or ides.

        Returns:
            Job: The job responsible for processing the assets.
        """
        asset_ids = [getattr(asset, "id", asset) for asset in as_collection(assets)]
        body = {
            "search": {
                "query": {
                    "terms": {
                        "_id": asset_ids
                    }
                }
            },
            "modules": as_collection(modules)
        }

        return self.app.client.post("/api/v3/assets/_search/reprocess", body)

    def get_asset(self, id):
        """
        Return the asset with the given unique Id.

        Args:
            id (str): The unique ID of the asset.

        Returns:
            Asset: The Asset
        """
        return Asset(self.app.client.get("/api/v3/assets/{}".format(id)))

    def update_labels(self, assets, add_labels=None, remove_labels=None):
        """
        Update the Labels on the given array of assets.

        Args:
            assets (mixed): An Asset, asset ID, or a list of either type.
            add_labels (list[Label]): A Label or list of Label to add.
            remove_labels (list[Label]): A Label or list of Label to remove.
        Returns:
            dict: An request status dict

        """
        ids = as_id_collection(assets)
        body = {}
        if add_labels:
            body['add'] = dict([(a, as_collection(add_labels)) for a in ids])
        if remove_labels:
            body['remove'] = dict([(a, as_collection(remove_labels)) for a in ids])
        if not body:
            raise ValueError("Must pass at least and add_labels or remove_labels argument")
        return self.app.client.put("/api/v3/assets/_batch_update_labels", body)

    def update_custom_fields(self, asset, values):
        """
        Set the values of custom metadata fields.

        Args:
            asset (Asset): The asset or unique Asset id.
            values (dict): A dictionary of values.

        Returns:
            dict: A status dictionary with failures or succcess
        """
        body = {
            "update": {
                as_id(asset): values
            }
        }
        return self.app.client.put("/api/v3/assets/_batch_update_custom_fields", body)

    def batch_update_custom_fields(self, update):
        """
        Set the values of custom metadata fields.

        Examples:
            {
                "asset-id1": {"shoe": "nike"},
                "asset-id2": {"country": "New Zealand"}
            }

        Args:
            update (dict): A dict o dicts which describe the
        Returns:
            dict: A status dictionary with failures or success
        """
        body = {
            'update': update
        }
        return self.app.client.put('/api/v3/assets/_batch_update_custom_fields', body)

    def download_file(self, stored_file, dst_file=None):
        """
        Download given file and store results in memory, or optionally
        a destination file.  The stored_file ID can be specified as
        either a string like "assets/<id>/proxy/image_450x360.jpg"
        or a StoredFile instance can be used.

        Args:
            stored_file (mixed): The StoredFile instance or its ID.
            dst_file (str): An optional destination file path.

        Returns:
            io.BytesIO instance containing the binary data or if
                a destination path was provided the size of the
                file is returned.

        """
        return self.app.client.download_file(stored_file, dst_file)

    def stream_file(self, stored_file, chunk_size=1024):
        """
        Streams a file by iteratively returning chunks of the file using a generator. This
        can be useful when developing web applications and a full download of the file
        before continuing is not necessary.

        Args:
            stored_file (mixed): The StoredFile instance or its ID.
            chunk_size (int): The byte sizes of each requesting chunk. Defaults to 1024.

        Yields:
            generator (File-like Object): Content of the file.

        """
        if isinstance(stored_file, str):
            path = stored_file
        elif isinstance(stored_file, StoredFile):
            path = stored_file.id
        else:
            raise ValueError("stored_file must be a string or StoredFile instance")

        url = self.app.client.get_url('/api/v3/files/_stream/{}'.format(path))
        response = requests.get(url, verify=self.app.client.verify,
                                headers=self.app.client.headers(), stream=True)

        for block in response.iter_content(chunk_size):
            yield block

    def get_sim_hashes(self, images):
        """
        Return a similarity hash for the given array of images.

        Args:
            images (mixed): Can be an file handle (opened with 'rb'), or
                path to a file.
        Returns:
            list of str: A list of similarity hashes.

        """
        return self.app.client.upload_files("/ml/v1/sim-hash",
                                            as_collection(images), body=None)

    def get_sim_query(self, images, min_score=0.75):
        """
        Analyze the given image files and return a SimilarityQuery which
        can be used in a search.

        Args:
            images (mixed): Can be an file handle (opened with 'rb'), or
                path to a file.
            min_score (float): A float between, the higher the value the more similar
                the results.  Defaults to 0.75

        Returns:
            SimilarityQuery: A configured SimilarityQuery
        """
        return SimilarityQuery(self.get_sim_hashes(images), min_score)


"""
A named tuple to define a ReprocessSearchResponse
"""
ReprocessSearchResponse = namedtuple('ReprocessSearchResponse', ["asset_count", "job"])
