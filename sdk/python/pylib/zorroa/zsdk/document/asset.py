import uritools
import mimetypes
from pathlib2 import Path

from ..util.std import file_exists
from ..document.base import Document
from ..document.sourcehandler import GcsSourceHandler, \
    FileSystemSourceHandler
from ..ofs import get_ofs


class Asset(Document):
    """Asset is a Document initialized by reading the standard attributes of a
    file.

    Information about the file is automatically added to the Document under
    the "source" attribute.

    * path - The full path to the file.
    * directory - Just the directory of the file.
    * filename - Just the filename and extension.
    * date - The created date of the file.
    * extension - the file extension, if any.
    * mediaType - the detected media type of the file. (aka mime type)
    * type - the super type of the file. (image, video, etc)
    * subType - the subtype of the file. (jpeg, mpeg)
    * fileSize - the size of the file in bytes.
    * owner - the owner permission of the file.
    * group = the group permission of the file.

    The unique ID for the source is a SHA1 hash of the absolute file path,
    truncated to
    an 128bit UUID, known as a UUID type 5.

    Args:
        path(str or Path): Path to a file to store as the Asset's source.
        *dics(AbstractDocumentIdContributor): Any number of
            DocumentIdContributors that will contribute to the idkey created
            for this Asset.

    """
    local_cache_field = 'tmp.source.local_path'

    @classmethod
    def from_document(cls, document):
        """Creates an Asset from an existing Document.

        Args:
            document (Document): Document to create an asset from.

        Returns:
            :obj:`zsdk.Asset`: New Asset object derived from the Document.

        """
        asset = Asset()
        asset.__dict__.update(document.__dict__)
        source_path = asset.get_attr('source.path')
        if source_path:
            asset.set_source(source_path)
        return asset

    def __init__(self, path=None, *dics):
        super(Asset, self).__init__()
        self.__path = None
        self.__source_handler = None
        if path:
            self.set_source(path, *dics)
            self.id = self.generate_id()

    def _set_source_handler(self, path):
        """Set the source handler class for the given file path.

        Args:
            path(str): The file path or Path object to check.

        """
        path = str(path)
        uri = uritools.urisplit(path)
        scheme = uri.scheme or 'file'
        if scheme == 'gs':
            self.__source_handler = GcsSourceHandler(path)
        elif scheme == 'file':
            self.__source_handler = FileSystemSourceHandler(path)
        else:
            raise TypeError('%s is an unknown scheme and cannot be used as an '
                            'Asset source.' % scheme)

    @property
    def source_path(self):
        """Get the source file path.

        Returns:
            str: The path to the source file.

        """
        return self.get_attr('source.path')

    @property
    def proxies(self):
        """Get the list of proxy files or an empty list if there are not any.

        Returns:
            :obj:`list` of :obj:`dict`': A list of proxy structures.

        """
        return self.get_attr('proxies.proxies', [])

    def get_proxy(self, width, height, mimetype):
        """Get a proxy of specific size and type.

        Args:
            width (int): proxy width
            height(int): proxy height
            mimetype: proxy mime type
        """
        def match(p):
            return p["width"] == width and p["height"] == height and p["mimetype"] == mimetype

        return self. _find_proxies(match)

    def is_clip(self):
        """Return true if this Asset is a clip

        Returns:
            bool: true if Asset is a clip.

        """
        return bool(self.get_attr('media.clip'))

    def set_source(self, path, *dics):
        """Sets the all pertinent metadata in the source namespace based on
        the given path and copies the file to the correct storage location
        based on the ofs backend given.

        Args:
            path (str or Path): Path to a file to store as the Asset's source.
            *dics (AbstractDocumentIdContributor): Any number of
                DocumentIdContributors that will contribute to the idkey
                created for this Asset.

        """
        self._set_source_handler(path)
        source = self.__source_handler.get_source_metadata()
        if dics:
            keys = []
            for dic in dics:
                keys.extend(dic.tokens())
                if dic.namespace():
                    self.set_attr(dic.namespace(), dic)
            source['idkey'] = '&'.join(keys)
        self.set_attr('source', source)

    def set_resolution(self, width, height):
        """Adds resolution metadata to the "media" namespace.

        Entries include height, width, aspect, and orientation.

        Args:
            width (number): Width of the asset in pixels.
            height (number): Height of the asset in pixels.

        """
        if width <= 0 or height <= 0:
            raise ValueError('Width and height must be greater than 0 to set '
                             'resolution metadata. %sx%s is invalid.' %
                             (width, height))
        self.set_attr('media.width', int(width))
        self.set_attr('media.height', int(height))
        aspect = round(float(width) / float(height), 2)
        self.set_attr('media.aspect', aspect)
        if aspect <= 0.95:
            orientation = 'portrait'
        elif aspect <= 1.05:
            orientation = 'square'
        else:
            orientation = 'landscape'
        self.set_attr('media.orientation', orientation)

    def create_clip(self, type, start, stop, name=None):
        """Create a clip Asset from this Asset. Return the new Asset. A clip
        asset becomes a child of the current asset.

        Args:
            type (str): The type of asset.
            start (float): The clip start value.
            stop (float): The clip stop value.
            name (str): The name of the clip if any.

        Returns:
            :obj:`Asset`: The new asset.

        """
        # Lazy loaded to avoid cyclic imports.
        from zorroa.zsdk import Clip

        clip = Clip(type=type, start=start, stop=stop, name=name,
                    parent=self.id)
        asset = Asset(self.source_path, clip)
        asset.set_attr('media.clip', clip)
        proxies = []
        for proxy_video in self.proxies:
            if proxy_video.get('mimetype', '').startswith('video'):
                proxies.append(proxy_video)
        if proxies:
            asset.set_attr('proxies.proxies', proxies)
        return asset

    def get_local_source_path(self):
        """Returns the path to a cached version of this Asset's source that
        exists locally.

        Returns:
            str: Path to a access the source file locally.

        """
        local_cache_location = self.get_attr(self.local_cache_field)
        if local_cache_location and Path(local_cache_location).exists():
            return local_cache_location
        elif self.__source_handler:
            local_cache_location = self.__source_handler.store_local_cache()
            if not Path(local_cache_location).exists():
                raise IOError(
                    'There was an error caching %s to the local disk.' %
                    local_cache_location)
            self.set_attr(self.local_cache_field, local_cache_location)
            return local_cache_location
        else:
            return None

    def open_source(self, mode='r'):
        """Open the source file and return an open file handle.

        Args:
            mode (str): The mode which to open the file in (r, w, rb, wb)

        Returns:
            :obj:`File`: An open file handle.

        """
        return open(self.get_local_source_path(), mode)

    def add_proxy(self, proxy):
        """
        Add the given proxy to the asset.  The proxy must include these fields:

            {
                "id": The OFS ID.
                "mimetype": The mime type.
                "width": The proxy media width.
                "height": The proxy media height.
            }

        Args:
            proxy (dict): A dictionary that describes a proxy.

        Returns:
            list[dict]: The full list of proxies.

        """
        if not isinstance(proxy, dict):
            raise ValueError("Proxy must be a dictionary of id, mimetype, width, height")
        if not proxy.get("id"):
            raise ValueError("The proxy is missing a 'id' property")
        if not proxy.get("mimetype"):
            raise ValueError("The proxy is missing a 'mimetype' property")
        if not proxy.get("width"):
            raise ValueError("The proxy is missing a 'width' property")
        if not proxy.get("height"):
            raise ValueError("The proxy is missing a 'height' property")

        proxies = self.proxies
        if proxy not in proxies:
            proxies.append(proxy)
        self.set_attr('proxies.proxies', proxies)
        return proxies

    def get_thumbnail_path(self):
        """Returns the path to a local thumbnail image of the asset.

        If there are no image proxies for this asset, then the source path
        of the image asset will be returned or None if the asset is not an image.

        Args:
            ofs (ObjectFileSystem): OFS to use for getting proxy images for
                this asset.

        Returns:
            str: Path to a thumbnail image stored locally.

        """
        path = None
        image_proxy = None

        # If there are image proxies return a cached local path to one.
        if self.proxies:
            for proxy in self.proxies:
                if proxy.get('mimetype', '').startswith('image'):
                    image_proxy = proxy
                    break
        if image_proxy:
            path = get_ofs().get(image_proxy['id']).sync_local()
        # If there are no proxies then return the local source path if it's
        # an image.
        if not path and mimetypes.guess_type(self.source_path)[0].startswith(
                'image'):
            path = self.get_local_source_path()

        if not path:
            raise Exception("Unable to find suitable thumbnail path.")

        if not file_exists(path):
            raise Exception(
                "The thumbnail path '%s' does not exist" % path)

        return path

    def _find_proxies(self, predicate):
        """
        Find proxies of given predicate.

        The method lists all the asset's proxies for which the predicate function is True.

        The default predicate is 'const True' and thus this function by default returns
        the same result as the self.proxies() property.

        Args:
            predicate: A function taking a proxy structure and returning a boolean. E.g.
            def predicate(p):
                return p["width"] == 512

        Returns:
            A list of proxies with the given predicate.
        """
        result = []
        for p in self.proxies:
            if predicate(p):
                result.append(p)
        return result
