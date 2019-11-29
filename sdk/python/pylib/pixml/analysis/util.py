import logging
import os
from urllib.parse import urlparse

from pathlib2 import Path

from .storage import file_cache
from ..app import app_from_env

__all__ = [
    "get_proxy_file",
    "add_proxy_file",
    "add_support_file"
]

logger = logging.getLogger(__name__)


def get_proxy_file(asset, min_width=1024, mimetype="image/", fallback=False):
    """
    Return a tuple containing a suitable proxy file or fallback to the source media.
    The first element of the tuple is the name of proxy file such as "proxy_200x200.jpg"
    or simply "source" if the source was selecte.

    Args:
        asset (Asset): an Asset instance
        min_width (int): The minimum width to accept for the proxy.
        mimetype (str): A mimetype filter, returns only files that start with this filter.
        fallback (bool): Fallback to the source if the proxy is not available.

    Returns:
        tuple: a tuple of name, path

    """
    files = asset.get_files(mimetype=mimetype, category="proxy")
    files = [file for file in files if file["attrs"]["width"] >= min_width]
    sorted(files, key=lambda f: f['attrs']['width'])

    if files:
        return files[0]["name"], file_cache.localize_remote_file(files[0])
    elif fallback and asset.get_attr("source.mimetype").startswith(mimetype):
        logger.warning("No suitable proxy mimetype={} minwidth={}, "
                       "falling back to source".format(mimetype, min_width))
        return 'source', file_cache.localize_remote_file(asset)
    else:
        raise ValueError("No suitable proxy file was found.")


def add_proxy_file(asset, path, size):
    """
    Add a proxy file with the proxy category to the given asset.

    Args:
        asset (Asset): The purpose of the file, ex proxy.
        path (str): The local path to the file.
        size (tuple of int): a tuple of width, height

    Returns:
        dict: a pixml file dictionary
    """
    _, ext = os.path.splitext(path)
    if not ext:
        raise ValueError("The path to the proxy file has no extension, but one is required.")
    name = "proxy_{}x{}{}".format(size[0], size[1], ext)
    return add_support_file(asset, path, "proxy", rename=name,
                            attrs={"width": size[0], "height": size[1]})


def add_support_file(asset, path, category, rename=None, attrs=None):
    """
    Add a support file to the asset and upload to PixelML storage.
    Also stores a copy into the local file cache.

    Args:
        asset (Asset): The purpose of the file, ex proxy.
        category (str): The purpose of the file, ex proxy.
        path (str): The local path to the file.
        rename (str): Rename the file to something better.
        attrs (dict): Arbitrary attributes to attach to the file.

    Returns:
        dict: a Pixml file dictionary.

    """
    app = app_from_env()
    name = rename or Path(path).name
    spec = {
        "name": name,
        "category": category,
        "attrs": {}
    }
    if attrs:
        spec["attrs"].update(attrs)

    # handle file:// urls
    path = urlparse(str(path)).path
    result = app.client.upload_file(
        "/api/v2/assets/{}/_files".format(asset.id), path, spec)

    # Store the path to the proxy in our local file storage
    # because a processor will need it down the line.
    file_cache.localize_pixml_file(result, path)

    # Ensure the file doesn't already exist in the metadata
    if not asset.get_files(name=name, category=category):
        files = asset.get_attr("files") or []
        files.append(result)
        asset.set_attr("files", files)

    return result
