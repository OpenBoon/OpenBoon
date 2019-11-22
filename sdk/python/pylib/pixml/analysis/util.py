import logging
import os

from pathlib2 import Path
from urllib.parse import urlparse

from ..app import app_from_env

__all__ = [
    "get_proxy_file",
    "add_proxy_file",
    "add_support_file"
]

logger = logging.getLogger(__name__)


def get_proxy_file(asset, min_width=1024, mimetype="image/", fallback=False):
    """
    Return a suitable proxy file or fallback to the source media.

    Args:
        asset:
        min_width:
        mimetype:
        fallback:

    Returns:

    """
    files = asset.get_files(mimetype=mimetype, category="proxy")
    files = [file for file in files if file["attrs"]["width"] >= min_width]
    sorted(files, key=lambda f: f['attrs']['width'])

    app = app_from_env()
    if files:
        return files[0]["name"], app.localize_remote_file(files[0])
    elif fallback and asset.get_attr("source.mimetype").startswith(mimetype):
        logger.warning("No suitable proxy mimetype={} minwidth={}, "
                       "falling back to source".format(mimetype, min_width))
        return 'source', app.localize_remote_file(asset)
    else:
        raise ValueError("No suitable proxy file was found.")


def add_proxy_file(asset, path, size):
    """
    Add a support file with the proxy category to the given asset.

    Args:
        asset (Asset): The purpose of the file, ex proxy.
        path (str): The local path to the file.
        size (tuple of int): a tuple of width, height
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
        dict: The new proxy information.

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
    path = urlparse(path).path
    result = app.client.upload_file(
        "/api/v2/assets/{}/_files".format(asset.id), path, spec)

    # Store the path to the proxy in our local file storage
    # because a processor will need it down the line.
    app.lfc.localize_pixml_file(result, path)

    if not asset.get_files(name=name, category=category):
        files = asset.get_attr("files") or []
        files.append(result)
        asset.set_attr("files", files)

    return result
