from ..asset import Asset
from ..search import AssetSearchResult, AssetSearchScroller
from ..util import as_collection


class AssetApp(object):

    def __init__(self, app):
        self.app = app

    def batch_import_files(self, assets):
        """
        Import a list of FileImport instances.

        Args:
            assets (list of FileImport): The list of files to import as Assets.

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
        body = {"assets": assets}
        return self.app.client.post("/api/v3/assets/_batch_create", body)

    def batch_upload_files(self, assets):
        """
        Batch upload a list of files and return a structure which contains
        an ES bulk response object, a list of failed file paths, a list of created
        asset Ids, and a processing jobId.

        Args:
            assets (list of FileUpload):

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
        assets = as_collection(assets)
        files = [asset.uri for asset in assets]
        body = {
            "assets": assets
        }
        return self.app.client.upload_files("/api/v3/assets/_batch_upload",
                                            files, body)

    def index(self, asset):
        """
        Re-index an existing asset.  The metadata for the entire asset
        is overwritten by the local copy.

        Args:
            asset (Asset): The asset

        Notes:
            Example return value:
                {
                  "_index" : "v4mtygyqqpsjlcnv",
                  "_type" : "_doc",
                  "_id" : "dd0KZtqyec48n1q1fniqVMV5yllhRRGx",
                  "_version" : 2,
                  "result" : "updated",
                  "_shards" : {
                    "total" : 1,
                    "successful" : 1,
                    "failed" : 0
                  },
                  "_seq_no" : 1,
                  "_primary_term" : 1
                }

        Examples:
            asset = app.assets.get_by_id(id)
            asset.set_attr("aux.my_field", 1000)
            asset.remove_attr("aux.other_field")
            app.assets.index(asset)

        Returns:
            dict: An ES update response.
        """
        return self.app.client.put("/api/v3/assets/{}/_index".format(asset.id),
                                   asset.document)

    def update(self, asset, doc):
        """
        Update a given Asset with a partial document dictionary.

        Args:
            asset: (mixed): An Asset object or unique asset id.
            doc: (dict): the changes to apply.

        Notes:
            Doc argument example:
                {
                    "aux": {
                        "captain": "kirk"
                    }
                }

            Example return value:
                {
                  "_index" : "9l0l2skwmuesufff",
                  "_type" : "_doc",
                  "_id" : "dd0KZtqyec48n1q1fniqVMV5yllhRRGx",
                  "_version" : 2,
                  "result" : "updated",
                  "_shards" : {
                    "total" : 1,
                    "successful" : 1,
                    "failed" : 0
                  },
                  "_seq_no" : 1,
                  "_primary_term" : 1
                }

        Returns
            dict: The ES update response object.
        """
        asset_id = getattr(asset, "id", None) or asset
        body = {
            "doc": doc
        }
        return self.app.client.post("/api/v3/assets/{}/_update".format(asset_id), body)

    def batch_index(self, assets):
        """
        Reindex multiple existing assets.  The metadata for the entire asset
        is overwritten by the local copy.

        Notes:
            Example return value:
                {
                  "took" : 11,
                  "errors" : false,
                  "items" : [ {
                    "index" : {
                      "_index" : "qjdjbpkvwg0sgusl",
                      "_type" : "_doc",
                      "_id" : "dd0KZtqyec48n1q1fniqVMV5yllhRRGx",
                      "_version" : 2,
                      "result" : "updated",
                      "_shards" : {
                        "total" : 1,
                        "successful" : 1,
                        "failed" : 0
                      },
                      "_seq_no" : 1,
                      "_primary_term" : 1,
                      "status" : 200
                    }
                  } ]
                }

        Returns:
            dict: An ES BulkResponse object.

        """
        body = dict([(a.id, a.document) for a in assets])
        return self.app.client.post("/api/v3/assets/_batch_index", body)

    def batch_update(self, docs):
        """
        Args:
            docs (dict): A dictionary of asset Id to document.

        Notes:
            Example request dictionary
                {
                    "assetId1": {
                        "doc": {
                            "aux": {
                                "captain": "kirk"
                            }
                        }
                    },
                    "assetId2": {
                        "doc": {
                            "aux": {
                                "captain": "kirk"
                            }
                        }
                    }
                }

        Returns:
            dict: An ES BulkResponse object.

        """
        return self.app.client.post("/api/v3/assets/_batch_update", docs)

    def delete(self, asset):
        """
        Delete the given asset.

        Args:
            asset (mixed): unique Id or Asset instance.

        Returns:
            An ES Delete response.

        """
        asset_id = getattr(asset, "id", None) or asset
        return self.app.client.delete("/api/v3/assets/{}".format(asset_id))

    def delete_by_query(self, search):
        """
        Delete assets by the given search.

        Args:
            search (dict): An ES search.

        Notes:
            Example Request:
                {
                    "query": {
                        "terms": {
                            "source.filename": {
                                "bob.jpg"
                            }
                        }
                    }
                }

        Returns:
            An ES delete by query response.

        """
        return self.app.client.delete("/api/v3/assets/_delete_by_query", search)

    def search(self, search=None):
        """
        Perform an asset search using the ElasticSearch query DSL.

        See Also:
            For search/query format.
            https://www.elastic.co/guide/en/elasticsearch/reference/6.4/search-request-body.html

        Args:
            search (dict): The ElasticSearch search to execute.
        Returns:
            AssetSearchResult - an AssetSearchResult instance.
        """
        return AssetSearchResult(self.app, search)

    def scroll_search(self, search=None, timeout="1m"):
        """
        Perform an asset scrolled search using the ElasticSearch query DSL.

        See Also:
            For search/query format.
            https://www.elastic.co/guide/en/elasticsearch/reference/6.4/search-request-body.html

        Args:
            search (dict): The ElasticSearch search to execute
            timeout (str): The scroll timeout.
        Returns:
            AssetSearchScroll - an AssetSearch instance.
        """
        return AssetSearchScroller(self.app, search, timeout)

    def get_by_id(self, id):
        """
        Return the asset with the given unique Id.

        Args:
            id (str): The unique ID of the asset.

        Returns:
            Asset: The Asset
        """
        return Asset(self.app.client.get("/api/v3/assets/{}".format(id)))
