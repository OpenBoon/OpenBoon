import pandas as pd
from collections import defaultdict

from boonsdk import app_from_env


def search_to_df(search=None,
                 num_assets=1000,
                 attrs=None,
                 descriptor='source.filename'):
    """Convert search results to DataFrame

    Args:
        search (AssetSearchResult): an AssetSearchResult instance from ES query
        num_assets (int): number of Assets to display (default: 1000)
        attrs (List[str]): attributes to get
        descriptor (str, default: source.filename): unique name to describe each row

    Returns:
        pd.DataFrame - DataFrame converted from assets
    """
    app = app_from_env()
    asset_dict = defaultdict(list)

    if search is None:
        search = app.assets.search({"size": num_assets})

    if attrs is None:
        attrs = ['media.height', 'media.width']

    for asset in search:
        src = asset.get_attr(descriptor)
        asset_dict[descriptor].append(src)
        for attr in attrs:
            a = asset.get_attr(attr)
            asset_dict[attr].append(a)
    df = pd.DataFrame(asset_dict)

    return df
