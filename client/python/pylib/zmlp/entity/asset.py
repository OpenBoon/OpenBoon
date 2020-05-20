import json
import logging
import os

from ..client import to_json
from ..util import as_collection

__all__ = [
    'Asset',
    'FileImport',
    'FileUpload',
    'Clip',
    'StoredFile',
    'FileTypes'
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

    def add_analysis(self, name, val):
        """Add an analysis structure to the document.

        Args:
            name (str): The name of the analysis
            val (mixed): the value/result of the analysis.

        """
        if not name:
            raise ValueError("Analysis requires a unique name")
        attr = "analysis.%s" % name
        if val is None:
            self.set_attr(attr, None)
        else:
            self.set_attr(attr, json.loads(to_json(val)))

    def get_analysis(self, name):
        """
        Return the the given analysis data under the the given name.

        Args:
            name (str): The pipeline module name that generated the data.

        Returns:
            dict: An arbitrary dictionary containing predictions, content, etc.

        """
        return self.get_attr("analysis.{}".format(name))

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

    def __init__(self, uri, attrs=None, clip=None, label=None):
        """
        Construct an FileImport instance which can point to a remote URI.

        Args:
            uri (str): a URI locator to the file asset.
            attrs (dict): A shallow key/value pair dictionary of starting point
                attributes to set on the asset.
            clip (Clip): Defines a subset of the asset to be processed, for example a
                page of a PDF or time code from a video.
            label (DataSetLabel): An optional dataset label which will add the file to
                a DataSet automatically.
        """
        super(FileImport, self).__init__()
        self.uri = uri
        self.attrs = attrs or {}
        self.clip = clip
        self.label = label

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        return {
            "uri": self.uri,
            "attrs": self.attrs,
            "clip": self.clip,
            "label": self.label
        }

    def __setitem__(self, field, value):
        self.attrs[field] = value

    def __getitem__(self, field):
        return self.attrs[field]


class FileUpload(FileImport):
    """
    FileUpload instances point to a local file that will be uploaded for analysis.
    """

    def __init__(self, path, attrs=None, clip=None, label=None):
        """
        Create a new FileUpload instance.

        Args:
            path (str): A path to a file, the file must exist.
            attrs (dict): A shallow key/value pair dictionary of starting point
                attributes to set on the asset.
            clip (Clip): Clip settings if applicable.
            label (DataSetLabel): An optional dataset label which will add the file to
                a DataSet automatically.
        """
        super(FileUpload, self).__init__(
            os.path.normpath(os.path.abspath(path)), attrs, clip, label)

        if not os.path.exists(path):
            raise ValueError('The path "{}" does not exist'.format(path))

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        return {
            "uri": self.uri,
            "clip": self.clip,
            "label": self.label
        }


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
        self.score = data.get("score", 0)
        self.inner_hits = data.get("inner_hits", [])

    @staticmethod
    def from_hit(hit):
        """
        Converts an ElasticSearch hit into an Asset.

        Args:
            hit (dict): An raw ES document

        Returns:
            Asset: The Asset.
        """
        return Asset({
            'id': hit['_id'],
            'score': hit.get('_score', 0),
            'document': hit.get('_source', {}),
            'inner_hits': hit.get('inner_hits', [])})

    @property
    def uri(self):
        """
        The URI of the asset.

        Returns:
            str: The URI of the data.

        """
        return self.get_attr("source.path")

    def add_file(self, stored_file):
        """
        Adds the StoredFile record to the asset's list of associated files.

        Args:
            stored_file (StoredFile): A file that has been stored in ZMLP

        Returns:
            bool: True if the file was added to the list, False if it was a duplicate.

        """
        # Ensure the file doesn't already exist in the metadata
        if not self.get_files(id=stored_file.id):
            files = self.get_attr("files") or []
            files.append(stored_file._data)
            self.set_attr("files", files)
            return True
        return False

    def get_files(self, name=None, category=None, mimetype=None, extension=None,
                  id=None, attrs=None, attr_keys=None, sort_func=None):
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
            list of StoredFile: A list of ZMLP file records.

        """
        result = []
        files = self.get_attr("files") or []
        for fs in files:
            match = True
            if id and not any((item for item in as_collection(id)
                               if fs["id"] == item)):
                match = False
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
                result.append(StoredFile(fs))

        if sort_func:
            result = sorted(result, key=sort_func)

        return result

    def get_thumbnail(self, level):
        """
        Return an thumbnail StoredFile record for the Asset. The level
        corresponds size of the thumbnail, 0 for the smallest, and
        up to N for the largest.  Levels 0,1,and 2 are smaller than
        the source media, level 3 or above  (if they exist) will
        be full resolution or higher images used for OCR purposes.

        To download the thumbnail call app.assets.download_file(stored_file)

        Args:
            level (int): The size level, 0 for smallest up to N.

        Returns:
            StoredFile: A StoredFile instance or None if no image proxies exist.
        """
        files = self.get_files(mimetype="image/", category="proxy",
                               sort_func=lambda f: f.attrs.get('width', 0))
        if not files:
            return None
        if level >= len(files):
            level = -1
        return files[level]

    def get_inner_hits(self, name):
        """
        Return any inner hits from a collapse query.

        Args:
            name (str): The inner hit name.

        Returns:
            list[Asset]:  A list of Assets.
        """
        try:
            return [Asset.from_hit(hit) for hit in self.inner_hits[name]['hits']['hits']]
        except KeyError:
            return []

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        return {
            "id": self.id,
            "uri": self.get_attr("source.path"),
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
    track attributes fo it to be considered a unique clip.

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
    def scene(time_in, time_out, track):
        """
        Return a video scene Clip with the given in/out points and a track name.

        Args:
            time_in: (float): The start time of the cut.
            time_out: (float): The end time of the cut.
            track: (str): An track label.  Videos can be clipified multiple ways
                by multiple types of services and labeling them with a track is
                useful for differentiating them.
        Returns:
            Clip: A scene Clip.

        """
        return Clip('scene', time_in, time_out, track)

    def __init__(self, type, start, stop, track=None):
        """Initialize a new clip.

        Args:
            type (str): The clip type, usually 'scene' or 'page' but it can be arbitrary.
            start (float): The start of the clip
            stop (float): The end of the clip,
            track (str): The track the clip belongs to.
        """

        self.type = type
        self.start = float(start)
        self.stop = float(stop)
        self.track = track

    def for_json(self):
        """Return a JSON serialized copy.

        Returns:
            :obj:`dict`: A json serializable dict.
        """
        serializable_dict = {}
        attrs = ['type', 'start', 'stop', 'track']
        for attr in attrs:
            if getattr(self, attr, None) is not None:
                serializable_dict[attr] = getattr(self, attr)
        return serializable_dict


class StoredFile(object):
    """
    The StoredFile class represents a supporting file that has been stored in ZVI.
    """

    def __init__(self, data):
        self._data = data

    @property
    def id(self):
        """
        The unique ID of the file.
        """
        return self._data['id']

    @property
    def name(self):
        """
        The file name..
        """
        return self._data['name']

    @property
    def category(self):
        """
        The file category.
        """
        return self._data['category']

    @property
    def attrs(self):
        """
        Arbitrary attributes.
        """
        return self._data['attrs']

    @property
    def mimetype(self):
        """
        The file mimetype.
        """
        return self._data['mimetype']

    @property
    def size(self):
        """
        The size of the file.
        """
        return self._data['size']

    @property
    def cache_id(self):
        """
        A string suitable for on-disk caching/filenames.  Replaces
        all slashes in id with underscores.
        """
        return self.id.replace("/", "_")

    def __str__(self):
        return "<StoredFile {}>".format(self.id)

    def __eq__(self, other):
        return other.id

    def __hash__(self):
        return hash(self.id)

    def for_json(self):
        """Return a JSON serialized copy.

        Returns:
            :obj:`dict`: A json serializable dict.
        """
        serializable_dict = {}
        attrs = self._data.keys()
        for attr in attrs:
            if getattr(self, attr, None) is not None:
                serializable_dict[attr] = getattr(self, attr)
        return serializable_dict


class FileTypes:
    """
    A class for storing the supported file types.
    """

    videos = frozenset(['mov', 'mp4', 'mpg', 'mpeg', 'm4v', 'webm', 'ogv', 'ogg', 'mxf'])
    """A set of supported video file formats."""

    images = frozenset(["bmp", "cin", "dpx", "gif", "jpg",
                        "jpeg", "exr", "png", "psd", "rla", "tif", "tiff"])
    """A set of supported image file formats."""

    documents = frozenset(['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'vsd', 'vsdx'])
    """A set of supported document file formats."""

    all = frozenset(videos.union(images).union(documents))

    """A set of all supported file formats."""

    @classmethod
    def resolve(cls, file_types):
        """
        Resolve a list of file extenions or types (images, documents, videos) to
        a supported list of extensions.

        Args:
            file_types (list): A list of file extensions, dot not included.

        Returns:
            list: The valid list of extensions from the given list

        """
        file_types = as_collection(file_types)
        if not file_types:
            return cls.all
        result = set()
        for file_type in file_types:
            if file_type in cls.all:
                result.add(file_type)
            else:
                exts = getattr(cls, file_type, None)
                if exts:
                    result.update(exts)

        return sorted(list(result))
