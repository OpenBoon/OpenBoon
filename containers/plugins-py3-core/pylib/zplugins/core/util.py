import logging
import os

logger = logging.getLogger(__name__)


def get_image_path(asset, ofs):
    """Return the path to an image representation for the given asset."""

    p_path = None
    proxy = asset.get_array_attr("proxies.proxies", [1, 0])
    if proxy:
        p_path = ofs.get(proxy['id']).path

    if not p_path:
        p_path = asset.get_local_source_path()

    if not p_path:
        raise Exception("Unable to find suitable path for image processing")

    if not os.path.exists(p_path):
        raise Exception("The path '%s' needed for image processing does not exist" % p_path)

    return p_path
