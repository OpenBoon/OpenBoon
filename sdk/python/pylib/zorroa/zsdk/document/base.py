# -*- coding: utf-8 -*-
import copy
import logging
import uuid

from ..util.std import as_collection

__all__ = [
    "Document",
    "generate_asset_id"
]

logger = logging.getLogger(__name__)


class Document(object):
    """The Document class is the base class for the data structure passed
    between Processors.  Underneath the Document is just a large nested
    dictionary with an associated unique ID. It is the responsibility of
    subclasses to calculate and set a unique ID.  The unique ID can be used
    to quickly retrieve the document at a later point.  Not setting the
    unique ID will result in one being randomly assigned if the document is
    indexed.

    The Document API provides a simple "dot notation" mechanism for setting
    deep values.

    .. code-block:: python

        d = Document()
        d.set_attr("foo.bar.bing", "bilbo")
        print d.dict()
        {
            "foo": {
                "bar": {
                    "bing": "bilbo"
                }
            }
        }
    """
    def __init__(self, data=None):
        if not data:
            data = {}
        self.id = data.get("id", None)
        self.document = data.get("document", {})
        self.replace = data.get("replace", False)

    def set_attr(self, attr, value):
        """Set the value of an attribute.

        Args:
            attr (str): The attribute name in dot notation format.
                ex: 'foo.bar'
            value (:obj:`object`): value: The value for the particular
                attribute. Can be any json serializable type.

        Returns (bool):
            True if successful, False if the value conflicts with an immutable
                field.

        """
        field_edits = self.get_field_edits()

        if attr in field_edits:
            return False
        self.__set_attr(attr, value)
        self.__apply_field_edits(field_edits)
        return self.get_attr(attr) == value

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
        field_edits = self.get_field_edits()
        if attr in field_edits:
            return False

        doc = self.document
        parts = attr.split(".")
        for k in parts[0:-1]:
            if not isinstance(doc, dict) or k not in doc:
                return False
            doc = doc.get(k)

        attr_name = parts[-1]
        try:
            del doc[attr_name]
            self.__apply_field_edits(field_edits)
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
            :obj:`mixed` The value of the attribute.

        """
        doc = self.document
        parts = attr.split(".")
        for k in parts:
            if not isinstance(doc, dict) or k not in doc:
                return default
            doc = doc.get(k)
        return doc

    def get_ref(self):
        """Returns a unique reference string that identifies the document.

        Returns:
            str: A unqiue string which contains the path and idkey values.
        """
        ref = self.get_attr('source.path')
        id_key = self.get_attr('source.idkey')
        if id_key:
            ref += '?%s' % id_key
        return ref

    def attr_exists(self, attr):
        """Return true if the given attribute exists.

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

    def get_array_attr(self, attr, idx, def_val=None):
        """Get the given element of an array or dictionary attribute.

        Args:
            attr (str): The name of the attribute.
            idx (int): The index of the array
            def_val (:obj:`mixed'): A default value if the index does not
                exist.

        Returns:
            :obj:`mixed`: The value of the index position of the requested
                array.

        """
        value = def_val
        col = self.get_attr(attr)
        if not col:
            return None
        for i in as_collection(idx):
            try:
                value = col[i]
                break
            except (IndexError, KeyError):
                pass
        return value

    def add_analysis(self, id, val):
        """Add an analysis structure to the document.

        Args:
            id (str): The name of the analysis
            val (:obj:`mixed`): the value/result of the analysis.

        """
        if not id or not val:
            raise ValueError("Analysis requires a unique ID and value")
        attr = "analysis.%s" % id
        self.set_attr(attr, val)

    def extend_list_attr(self, attr, items):
        """Adds the given items to the given attr. The attr must be a list or set.

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

    def add_keywords(self, namespace, words):
        """Add the given list of keywords to a .keywords attribute in the given
        namespace.

        Args:
            namespace (str): The namespace or attribute prefix.
            words: (:obj:`list` of str): A list of new words.

        """
        attr = namespace + '.keywords'
        self.extend_list_attr(attr, words)

    def add_content(self, namespace, content):
        """Add a content attribute to the given namespace.

        Content has no .raw indexing.

        Args:
            namespace (str): The namespace to add the 'content' attribute to.
            content (str): some content.

        """
        attr = namespace + '.content'
        self.extend_list_attr(attr, content)

    def dict(self):
        """Get the internal document.

        Returns:
            :obj:`dict`: The internal document dictionary.

        """
        return self.document

    def get_field_edits(self):
        """
        Return a dict of all field edits for this asset. The dict will
        be in the format of 'attr name' to 'value'.

        Returns:
            :obj:`dict`: A dictionary of field edits.

        """
        return copy.deepcopy(
            dict((k, self.get_attr(k)) for k in self.get_attr('system.fieldEdits') or list()))

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        return {"id": self.id,
                "document": self.document,
                "replace": self.replace}

    def generate_id(self, attr="source.path"):
        """Generate a unique ID for this document.

        Args:
            attr (str): The name of the attr to generate the ID from

        Returns:
            str: The UUID generated from the attr.

        """
        key = str(self.get_attr(attr))
        if not key:
            raise ValueError("Document must have '%s' attr to generate an ID" %
                             attr)

        return generate_asset_id(key, self.get_attr("source.idkey"))

    def __set_attr(self, attr, value):
        """A private set_attr method that handles just the setting of the
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

    def __apply_field_edits(self, field_edits):
        """
        Apply the given field edit dictionary to the document.

        Args:
            field_edits (dict): The field edits to apply.

        """
        for field in field_edits:
            self.__set_attr(field, field_edits[field])


def generate_asset_id(from_value, idkey=None):
    """
    Generate an unique asset Id using the UUID5 methodology.

    Args:
        from_value (str): The value to hash
        idkey (str): Additional unique values to hash in the case the from_value is not unique.

    Returns:
        str: A UUID hash of the from_value and idkey.

    """
    # The source.idkey field contains a string which when combined with
    # the from_value should generate a unique id.
    if idkey:
        from_value = "%s?%s" % (from_value, idkey)

    # The underlying algorithm can not be changed without the risk
    # of generating duplicate assets.
    #
    return str(uuid.uuid5(uuid.NAMESPACE_URL, from_value))
