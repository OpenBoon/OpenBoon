import os
from collections import namedtuple

from boonflow import AssetProcessor
from boonflow.proxy import get_proxy_level_path
from boonai_analysis.utils.simengine import SimilarityEngine

package_directory = os.path.dirname(os.path.abspath(__file__))

Batch = namedtuple('Batch', ['data'])


class ZviSimilarityProcessor(AssetProcessor):
    """
    make a hash with ResNet
    """

    namespace = "zvi-image-similarity"

    # MXNet is not thread safe.
    use_threads = False

    model_path = "/models/resnet-152"

    def __init__(self):
        super(ZviSimilarityProcessor, self).__init__()
        self.engine = None

    def init(self):
        self.engine = SimilarityEngine(self.model_path)

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 0)
        mxhash = self.engine.calculate_hash(p_path)
        struct = {
            'type': 'similarity',
            'simhash': mxhash
        }

        asset.add_analysis(self.namespace, struct)
