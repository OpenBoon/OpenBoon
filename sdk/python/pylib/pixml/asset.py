import logging

from .util import as_collection

__all__ = [
    "Asset",
    "AssetSpec",
    "Clip"
]

logger = logging.getLogger(__name__)


class AssetBase(object):

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
        A private set_attr method that handles just the setting of the
        attribute without any field edit protection.

        This gets called from set_attr to avoid infinite looping.

        Args:
            attr (str): The attribute name in dot notation format.
                ex: 'foo.bar'
            value (:obj:`object`): value: The value for the particular
                attribute.  Can be any json serializable type.

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


class AssetSpec(AssetBase):
    """
    An AssetSpec is used to create a new Asset.
    """
    def __init__(self, uri, clip=None):
        """
        Construct an AssetSpec

        Args:
            uri (str): a URI locator to the file asset.
            clip (Clip): Defines a subset of the asset to be processed, for example a
                page of a PDF or time code from a video.
        """
        super(AssetSpec, self).__init__()
        self.uri = uri
        self.clip = clip

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        return {
            "uri": self.uri,
            "document": self.document,
            "clip": self.clip
        }


class Asset(AssetBase):

    def __init__(self, data):
        super(Asset, self).__init__()
        """

        Args:
            id:
            document:
        """
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

    def get_files(self, name=None, category=None, mimetype=None, extension=None, attrs=None):
        """
        Return all stored files associated with this asset.  Optionally
        filter the results.

        Args:
            name (str): The associated files name.
            category (str): The associated files category, eg proxy, backup, etc.
            mimetype (str): The mimetype must start with this string.
            extension: (str): The file name must have the given extension.
            attrs (dict): The file must have all of the given attributes.

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
            if attrs:
                for k, v in attrs.items():
                    if fs.get("attrs", {}).get(k) != v:
                        match = False
            if match:
                result.append(fs)
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


class Clip(object):
    """
    A Clip object is used to define some frame range
    or sub-section of an asset.
    """
    def __init__(self, type, start, stop, timeline=None):
        """Initialize a new clip.

        Args:
            type (str): The clip type (image, video, page)
            start (float): The start of the clip
            stop (float): The end of the clip
            timeline (str): Put the clip on 1 unique timeline in case it
                collides with other clips with similar in/out points.
        """
        self.type = type
        self.start = float(start)
        self.stop = float(stop)
        self.length = max(1.0, self.stop - self.start + 1.0)
        self.timeline = timeline

    def tokens(self):
        """Return tokens that make the clip unique.

        Returns:
            :obj:`list` of str: A list of tokens.

        """
        return ["start=%0.3f" % self.start, "stop=%0.3f" % self.stop]

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
