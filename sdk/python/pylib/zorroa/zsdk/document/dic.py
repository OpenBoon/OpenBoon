
class AbstractDocumentIdContributor(object):
    """A class which provides tokens that contribute to an assets unique ID."""

    def tokens(self):
        """Returns a list of tokens to use when creating a unique document id.

        Returns:
            :obj:`list` of str: List of tokens.

        """
        raise NotImplementedError()

    def namespace(self):
        """Metadata namespace this DIC should be stored in on the Document.

        Returns:
            str: Metadata namespace.

        """
        raise NotImplementedError()


class Clip(AbstractDocumentIdContributor):
    """A Clip object is used to define some frame range or sub-section of an
    asset.
    """
    def __init__(self, parent, type, start, stop, name=None):
        """Initialize a new clip.

        Args:
            parent (:obj:`Asset`): The parent asset.
            type (str): The clip type
            start (float): The start of the clip
            stop (float): The end of the clip
            name (str): The name of the clip. Defaults to None

        """
        super(Clip, self).__init__()
        self.name = name
        self.type = type
        self.start = float(start)
        self.stop = float(stop)
        self.length = max(1.0, self.stop - self.start + 1.0)
        self.parent = parent

    def tokens(self):
        """Return tokens that make the clip unique.

        Returns:
            :obj:`list` of str: A list of tokens.

        """
        return ["start=%0.3f" % self.start, "stop=%0.3f" % self.stop]

    def namespace(self):
        """Return the clip attribute namespace.

        Returns:
            str: The name of the clip namespace

        """
        return "media.clip"

    def for_json(self):
        """Return a JSON serializale copy.

        Returns:
            :obj:`dict`: A json serializable dict.
        """
        serializable_dict = {}
        attrs = ['name', 'type', 'start', 'stop', 'length', 'parent']
        for attr in attrs:
            if getattr(self, attr, None) is not None:
                serializable_dict[attr] = getattr(self, attr)
        return serializable_dict

    def __str__(self):
        return self.name
