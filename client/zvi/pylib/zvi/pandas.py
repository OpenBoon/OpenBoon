import pandas as pd
from collections import defaultdict


def search_to_df(search=None, attrs=None, descriptor='source.filename'):
    """Convert search results to DataFrame

    Args:
        search (AssetSearchResult): an AssetSearchResult instance from ES query
        attrs (List[str]): attributes to get
        descriptor (str, default: source.filename): unique name to describe each row

    Returns:
        pd.DataFrame - DataFrame converted from assets
    """
    asset_dict = defaultdict(list)

    for asset in search:
        src = asset.get_attr(descriptor)
        asset_dict[descriptor].append(src)
        for attr in attrs:
            a = asset.get_attr(attr)
            asset_dict[attr].append(a)
    df = pd.DataFrame(asset_dict)

    return df
