import logging

from pathlib2 import Path

import pixml

logger = logging.getLogger(__name__)


def add_file(asset, category, path, rename=None, attrs=None):
    """
    Add a support file to the asset and optionally upload
    the file to Pixml Storage.

    Args:
        asset (Asset): The purpose of the file, ex proxy.
        category (str): The purpose of the file, ex proxy.
        path (str): The local path to the file.
        rename (str): Rename the file to something better.
    Returns:
        dict: The new proxy information.

    """
    lfc = pixml.analysis.local_file_cache()
    app = pixml.app.from_env()
    name = rename or Path(path).name
    spec = {
        "name": name,
        "category": category,
        "attrs": {}
    }
    if attrs:
        spec.attrs.update(attrs)

    result = app.client.post("/api/v2/assets/_files", spec)
    # Store the path to the proxy in our local file storage
    # because if someone needs one they will check there.
    lfc.localize_file_storage(result, path)

    files = asset.get_attr("files") or []
    files.append(result)
    asset.set_attr("files", files)
