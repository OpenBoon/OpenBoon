import os
import magic
import logging

from pathlib2 import Path

from zorroa.zsdk.ofs import get_ofs
from zorroa.zsdk.util.std import file_exists
from zplugins.util.media import media_size

logger = logging.getLogger(__name__)


def add_proxy(asset, path, symlink=False):
    """
    Add a proxy file to the asset.  Size and media type are auto-detected.

    Args:
        asset (Asset): The asset to add a proxy to.
        path (str): The a path to the file to add as a proxy.
        symlink (bool): If true, make a symlink to the file, do not copy.
    Returns:
        dict: The new proxy information.

    """
    extension = Path(path).suffix.strip('.')
    mimetype = magic.detect_from_filename(path).mime_type
    width, height = media_size(path)
    object_file = get_ofs().prepare('asset', asset, "proxy_%dx%d.%s" %
                                    (width, height, extension))
    obj_path = object_file.path

    # Important:
    # If a file exists in the OFS remove it first or
    # else in copy mode we could end up overwriting source
    # data if the OFS file is a symlink
    if file_exists(obj_path):
        os.unlink(obj_path)

    if symlink:
        object_file.mkdirs()
        try:
            os.symlink(path, obj_path)
        except OSError:
            logger.warn("Failure creating symlink '{}' to '{}', OFS path already exists".format(
                path, obj_path))
    else:
        object_file.store(path)

    proxy = {'id': object_file.id,
             'width': width,
             'height': height,
             'mimetype': mimetype}
    asset.add_proxy(proxy)
    return proxy


def add_proxy_file(asset, path):
    """
    Add a proxy file to the asset.  Size and media type are auto-detected. The
    given file path is copied into an OFS location.

    Args:
        asset (Asset): The asset to add a proxy to.
        path: (str): The a path to the file to add as a proxy.

    Returns:
        dict: The new proxy information.

    """
    return add_proxy(asset, path, False)


def add_proxy_link(asset, path):
    """
    Add a proxy file to the asset.  Size and media type are auto-detected. The
    given file path is symlinked into an OFS location.

    Args:
        asset (Asset): The asset to add a proxy to.
        path: (str): The a path to the file to add as a proxy.

    Returns:
        dict: The new proxy information.

    """
    return add_proxy(asset, path, True)
