
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
