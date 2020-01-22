import json
import logging
import os

from .client import SearchResult, ZmlpJsonEncoder
from .elements import Element
from .util import as_collection

__all__ = [
    "Asset",
    "FileImport",
    "FileUpload",
    "AssetApp",
    "Clip"
]

logger = logging.getLogger(__name__)


class DocumentMixin(object):
    """
    A Mixin class which provides easy access to a deeply nested dictionary.
    """

    def __init__(self):
        self.document = {}

    def set_attr(self, attr, value):
        """Set the value of an attribute.

        Args:
            attr (str): The attribute name in dot notation format.
                ex: 'foo.bar'
            value (:obj:`object`): value: The value for the particular
                attribute. Can be any json serializable type.
        """
        self.__set_attr(attr, value)

    def del_attr(self, attr):
        """
        Delete the attribute from the document.  If the attribute does not exist
        or is protected by a manual field edit then return false.  Otherwise,
        delete the attribute and return true.

        Args:
            attr (str): The attribute name.

        Returns:
            bool: True if the attribute was deleted.

        """
        doc = self.document
        parts = attr.split(".")
        for k in parts[0:-1]:
            if not isinstance(doc, dict) or k not in doc:
                return False
            doc = doc.get(k)

        attr_name = parts[-1]
        try:
            del doc[attr_name]
            return not self.attr_exists(attr)
        except KeyError:
            return False

    def get_attr(self, attr, default=None):
        """Get the given attribute to the specified value.

        Args:
            attr (str): The attribute name in dot notation format.
                ex: 'foo.bar'
            default (:obj:`mixed`) The default value if no attr exists.

        Returns:
            mixed: The value of the attribute.

        """
        doc = self.document
        parts = attr.split(".")
        for k in parts:
            if not isinstance(doc, dict) or k not in doc:
                return default
            doc = doc.get(k)
        return doc

    def attr_exists(self, attr):
        """
        Return true if the given attribute exists.

        Args:
            attr (str): The name of the attribute to check.

        Returns:
            bool: true if the attr exists.

        """
        doc = self.document
        parts = attr.split(".")
        for k in parts[0:len(parts) - 1]:
            if k not in doc:
                return False
            doc = doc.get(k)
        return parts[-1] in doc

    def add_analysis(self, id, val):
        """Add an analysis structure to the document.

        Args:
            id (str): The name of the analysis
            val (mixed): the value/result of the analysis.

        """
        if not id or not val:
            raise ValueError("Analysis requires a unique ID and value")
        attr = "analysis.%s" % id
        self.set_attr(attr, val)

    def add_element(self, element):
        """
        Add a new Element instance to this Asset instance.  The Element is not
        saved to the serve until the asset is re-indexed.

        Args:
            element (Element): An element instance.

        """
        if not isinstance(element, Element):
            raise ValueError("Could not add element, value was not an Element instance.")

        elements = self.get_attr("elements") or []
        elements.append(json.loads(json.dumps(element, cls=ZmlpJsonEncoder)))
        self.set_attr("elements", elements)

    def extend_list_attr(self, attr, items):
        """
        Adds the given items to the given attr. The attr must be a list or set.

        Args:
            attr (str): The name of the attribute
            items (:obj:`list` of :obj:`mixed`): A list of new elements.

        """
        items = as_collection(items)
        all_items = self.get_attr(attr)
        if all_items is None:
            all_items = set()
            self.set_attr(attr, all_items)
        try:
            all_items.update(items)
        except AttributeError:
            all_items.extend(items)

    def __set_attr(self, attr, value):
        """
        Handles setting an attribute value.

        Args:
            attr (str): The attribute name in dot notation format.  ex: 'foo.bar'
            value (mixed): The value for the particular attribute.
                Can be any json serializable type.
        """
        doc = self.document
        parts = attr.split(".")
        for k in parts[0:len(parts) - 1]:
            if k not in doc:
                doc[k] = {}
            doc = doc[k]
        if isinstance(value, dict):
            doc[parts[-1]] = value
        else:
            try:
                doc[parts[-1]] = value.for_json()
            except AttributeError:
                doc[parts[-1]] = value

    def __setitem__(self, field, value):
        self.set_attr(field, value)

    def __getitem__(self, field):
        return self.get_attr(field)


class FileImport(object):
    """
    An FileImport is used to import a new file and metadata into ZMLP.
    """

    def __init__(self, uri, attrs=None, clip=None):
        """
        Construct an FileImport instance which can point to a remote URI.

        Args:
            uri (str): a URI locator to the file asset.
            attrs (dict): A shallow key/value pair dictionary of starting point
                attributes to set on the asset.
            clip (Clip): Defines a subset of the asset to be processed, for example a
                page of a PDF or time code from a video.
        """
        super(FileImport, self).__init__()
        self.uri = uri
        self.attrs = attrs or {}
        self.clip = clip

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        return {
            "uri": self.uri,
            "attrs": self.attrs,
            "clip": self.clip
        }

    def __setitem__(self, field, value):
        self.attrs[field] = value

    def __getitem__(self, field):
        return self.attrs[field]


class FileUpload(FileImport):
    """
    FileUpload instances point to a local file that will be uploaded for analysis.
    """

    def __init__(self, path, attrs=None, clip=None):
        """
        Create a new FileUpload instance.

        Args:
            path (str): A path to a file, the file must exist.
            attrs (dict): A shallow key/value pair dictionary of starting point
                attributes to set on the asset.
            clip (Clip): Clip settings if applicable.
        """
        super(FileUpload, self).__init__(os.path.normpath(os.path.abspath(path)), attrs, clip)

        if not os.path.exists(path):
            raise ValueError('The path "{}" does not exist'.format(path))


class Asset(DocumentMixin):
    """
    An Asset represents a single processed file or a clip/segment of a
    file. Assets start out in the 'CREATED' state, which indicates
    they've been created by not processed.  Once an asset has been processed
    and augmented with files created by various analysis modules, the Asset
    will move into the 'ANALYZED' state.
    """

    def __init__(self, data):
        super(Asset, self).__init__()
        if not data:
            raise ValueError("Error creating Asset instance, Assets must have an id.")
        self.id = data.get("id")
        self.document = data.get("document", {})

    @property
    def uri(self):
        """
        The URI of the asset.

        Returns:
            str: The URI of the data.

        """
        return self.get_attr("source.path")

    def get_files(self, name=None, category=None, mimetype=None, extension=None,
                  attrs=None, attr_keys=None, sort_func=None):
        """
        Return all stored files associated with this asset.  Optionally
        filter the results.

        Args:
            name (str): The associated files name.
            category (str): The associated files category, eg proxy, backup, etc.
            mimetype (str): The mimetype must start with this string.
            extension: (str): The file name must have the given extension.
            attrs (dict): The file must have all of the given attributes.
            attr_keys: (list): A list of attribute keys that must be present.
            sort_func: (func): A lambda function for sorting the result.
        Returns:
            list of dict: A list of ZMLP file records.

        """
        result = []
        files = self.get_attr("files") or []
        for fs in files:
            match = True
            if name and not any((item for item in as_collection(name)
                                 if fs["name"] == item)):
                match = False
            if category and not any((item for item in as_collection(category)
                                     if fs["category"] == item)):
                match = False
            if mimetype and not any((item for item in as_collection(mimetype)
                                     if fs["mimetype"].startswith(item))):
                match = False
            if extension and not any((item for item in as_collection(extension)
                                      if fs["name"].endswith("." + item))):
                match = False

            file_attrs = fs.get("attrs", {})
            if attr_keys:
                if not any(key in file_attrs for key in as_collection(attr_keys)):
                    match = False

            if attrs:
                for k, v in attrs.items():
                    if file_attrs.get(k) != v:
                        match = False
            if match:
                result.append(fs)

        if sort_func:
            result = sorted(result, key=sort_func)

        return result

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        return {
            "id": self.id,
            "document": self.document
        }

    def __str__(self):
        return "<Asset id='{}'/>".format(self.id)

    def __repr__(self):
        return "<Asset id='{}' at {}/>".format(self.id, hex(id(self)))

    def __hash__(self):
        return hash(self.id)

    def __eq__(self, other):
        if not getattr(other, "id"):
            return False
        return other.id == self.id


class Clip(object):
    """
    A Clip object is used to define a subsection of a file/asset that should be
    processed, for example a particular page of a PDF or a section of a movie.

    Each clip of an Asset needs to have a unique type, start, stop, and optionally
    timeline attributes fo it to be considered a unique clip.

    """

    @staticmethod
    def page(page_num):
        """
        Return a standard 'page' clip for the given page.

        Args:
            page_num (int): The page number

        Returns:
            Clip: The page clip.

        """
        return Clip('page', page_num, page_num)

    @staticmethod
    def scene(time_in, time_out, timeline):
        """
        Return a video scene Clip with the given in/out points and a timeline name.

        Args:
            time_in: (float): The start time of the cut.
            time_out: (float): The end time of the cut.
            timeline: (str): An timeline label.  Videos can be clipified multiple ways
                by multiple types of services and labeling them with a timeline is
                useful for differentiating them.
        Returns:
            Clip: A scene Clip.

        """
        return Clip('scene', time_in, time_out, timeline)

    def __init__(self, type, start, stop, timeline=None):
        """Initialize a new clip.

        Args:
            type (str): The clip type, usually 'scene' or 'page' but it can be arbitrary.
            start (float): The start of the clip
            stop (float): The end of the clip,
            timeline (str): Used when multiple type of clipification on a video occur.
        """

        self.type = type
        self.start = float(start)
        self.stop = float(stop)
        self.timeline = timeline

    def for_json(self):
        """Return a JSON serialized copy.

        Returns:
            :obj:`dict`: A json serializable dict.
        """
        serializable_dict = {}
        attrs = ['type', 'start', 'stop', 'timeline']
        for attr in attrs:
            if getattr(self, attr, None) is not None:
                serializable_dict[attr] = getattr(self, attr)
        return serializable_dict


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
        asset_id = getattr(asset, "id", "") or asset
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

    def search(self, search=None, raw=False):
        """
        Perform an asset search using the ElasticSearch query DSL.  Note that for
        load and security purposes, not all ElasticSearch search options are accepted.

        See Also:
            For search/query format.
            https://www.elastic.co/guide/en/elasticsearch/reference/6.4/search-request-body.html

        Args:
            search (dict): The ElasticSearch search to execute
            raw (bool): Return the raw ElasticSearch dict result rather than a SearchResult
        Returns:
            mixed: A SearchResult containing assets or in raw mode an
                ElasticSearch search result dictionary.
        """
        body = {
            'search': search,
        }
        rsp = self.app.client.post("/api/v3/assets/_search", body)
        if raw:
            return rsp
        else:
            rsp["hits"]["offset"] = search.get("from", 0)
            return SearchResult(rsp, Asset)

    def get_by_id(self, id):
        """
        Return the asset with the given unique Id.

        Args:
            id (str): The unique ID of the asset.

        Returns:
            Asset: The Asset
        """
        return Asset(self.app.client.get("/api/v3/assets/{}".format(id)))
