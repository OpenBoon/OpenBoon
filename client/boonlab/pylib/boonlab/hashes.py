import random
import numpy as np
from boonsdk import app_from_env


def read_hash_as_vectors(search=None,
                         attr='analysis.boonai-image-similarity.simhash',
                         percentage=100,
                         verbose=True):
    """This function reads a similarity hash from a Zorroa server into a numpy array.
    search: a search as returned bu archivist.AssetSearch(). If empty, an empty search will be used.
    attr: the attribute name of the hash to use.

    Returns a numpy array, a list of attribute IDs, a list of labels, and a list of legends
    """

    app = app_from_env()

    # If the provided search is None, create the empty search
    if search is None:
        search = app.assets.search({"size": 10000})

    if len(search.assets) == 0:
        print('ERROR: no assets found with the given attributes. Nothing to do.')
        return

    if verbose:
        print("About to process %d assets" % int(len(search.assets)*percentage/100.))

    random.seed(42)

    all_data = []
    assets = []
    for a in search.assets:
        if random.random()*100 < percentage:
            num_hash = []
            hash = a.get_attr(attr)
            if hash is not None:
                for char in hash:
                    num_hash.append(ord(char))
                all_data.append(num_hash)
                assets.append(a)

    x = np.asarray(all_data, dtype=np.float64)

    return x, assets
