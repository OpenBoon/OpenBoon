import tempfile
import pandas as pd
from collections import defaultdict

from zmlp import app_from_env


class ConvertSearchResults:
    """Convert search results into different formats"""

    def __init__(self, search=None, num_assets=1000, attrs=None, descriptor='source.filename'):
        """
        Convert search results into different formats

        Args:
            search (AssetSearchResult): an AssetSearchResult instance from ES query
            num_assets (int): number of Assets to display (default: 1000)
            attrs (List[str]): attributes to get
            descriptor (str, default: source.filename): unique name to describe each row
        """
        self.search = search
        self.num_assets = num_assets
        self.attrs = attrs
        self.descriptor = descriptor
        self.app = app_from_env()

    def to_dict(self):
        """
        Convert search results to dict

        Returns:
            dict
        """
        asset_dict = defaultdict(list)

        if self.search is None:
            self.search = self.app.assets.search({"size": self.num_assets})

        if self.attrs is None:
            self.attrs = ['media.height', 'media.width']

        for asset in self.search:
            src = asset.get_attr(self.descriptor)
            asset_dict[self.descriptor].append(src)
            for attr in self.attrs:
                a = asset.get_attr(attr)
                asset_dict[attr].append(a)

        return asset_dict

    def to_df(self):
        """
        Convert search results to DataFrame

        Returns:
            pd.DataFrame - DataFrame converted from assets
        """
        asset_dict = self.to_dict()
        df = pd.DataFrame(asset_dict)

        return df

    def to_csv(self, output_file=None):
        """Convert search results to CSV

        Args:
            output_file (str): output_file path

        Returns:
            (str) CSV output file path (in case output_file is not specified)
        """
        if not output_file:
            _, output_file = tempfile.mkstemp(".csv")

        df = self.to_df()
        df.to_csv(output_file)

        return output_file
