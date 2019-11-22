import logging

from pathlib2 import Path

import pixml

logger = logging.getLogger(__name__)


def add_file(asset, category, path, rename=None, attrs=None):
    """
    Add a support file to the asset and upload to PixelML storage.

    Args:
        asset (Asset): The purpose of the file, ex proxy.
        category (str): The purpose of the file, ex proxy.
        path (str): The local path to the file.
        rename (str): Rename the file to something better.
        attrs (dict): Arbitrary attributes to attach to the file.
    Returns:
        dict: The new proxy information.

    """
    app = pixml.app.app_from_env()
    name = rename or Path(path).name
    spec = {
        "name": name,
        "category": category,
        "attrs": {}
    }
    if attrs:
        spec["attrs"].update(attrs)

    result = app.client.upload_file(
        "/api/v2/assets/{}/_files".format(asset.id), path, spec)

    # Store the path to the proxy in our local file storage
    # because a processor will need it down the line.
    app.lfc.localize_pixml_file(result, path)

    files = asset.get_attr("files") or []
    files.append(result)
    asset.set_attr("files", files)
