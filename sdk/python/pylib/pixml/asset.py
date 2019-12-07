import logging
import os

from .rest import SearchResult
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
        for k in parts[0:len(parts)-1]:
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
        for k in parts[0:len(parts)-1]:
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

    def __getitem__(self, attr):
        return self.get_attr(attr)


class FileImport(object):
    """
    An FileImport is used to import a new file and metdata into PixelML.
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
        super(FileUpload, self).__init__(path, attrs, clip)
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
            list of dict: A list of pixml file records.

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
    def __init__(self, type, start, stop, timeline=None):
        """Initialize a new clip.

        Args:
            type (str): The clip type, usually video or page.
            start (float): The start of the clip
            stop (float): The end of the clip,
            timeline (str): When an asset is clipified multiple ways, use the timeline
                to differentiate or else elements may collide.
        """
        self.type = type
        self.start = float(start)
        self.stop = float(stop)
        self.timeline = timeline
        self.length = max(1.0, self.stop - self.start + 1.0)

    def for_json(self):
        """Return a JSON serialized copy.

        Returns:
            :obj:`dict`: A json serializable dict.
        """
        serializable_dict = {}
        attrs = ['type', 'start', 'stop', 'length', 'timeline']
        for attr in attrs:
            if getattr(self, attr, None) is not None:
                serializable_dict[attr] = getattr(self, attr)
        return serializable_dict


class AssetApp(object):

    def __init__(self, app):
        self.app = app

    def import_files(self, assets):
        """
        Import a list of FileImport instances.

        Args:
            assets (list of FileImport): The list of files to import as Assets.

        Returns:
            dict: A dictionary containing the provisioning status of each asset,
                a list of assets to be processed, and a analysis job id.

        """
        body = {"assets": assets}
        return self.app.client.post("/api/v3/assets/_batchCreate", body)

    def upload_files(self, assets):
        """
        Batch upload a list of files.

        Args:
            assets (list of FileUpload):

        Returns:

        """
        assets = as_collection(assets)
        files = [asset.uri for asset in assets]
        body = {
            "assets": assets
        }
        return self.app.client.upload_files("/api/v3/assets/_batchUpload",
                                            files, body)

    def search(self, search=None, deep_query=None, raw=False):
        """
        Perform an asset search using the ElasticSearch query DSL.  Note that for
        load and security purposes, not all ElasticSearch search options are accepted.

        See Also:
            For search/query format.
            https://www.elastic.co/guide/en/elasticsearch/reference/6.4/search-request-body.html

        Args:
            search (dict): The ElasticSearch search to execute
            deep_query (dict): An ElasticSearch query that will be applied to deep
                analysis elements.
            raw (bool): Return the raw ElasticSearch dict result rather than a SearchResult
        Returns:
            mixed: A SearchResult containing assets or in raw mode an
                ElasticSearch search result dictionary.
        """
        body = {
            'search': search,
            'deepQuery': deep_query
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
