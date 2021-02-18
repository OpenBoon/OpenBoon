import os
import unittest

from mlbbq.simhash import get_similarity_hash, SimilarityModel

test_file = os.path.dirname(__file__) + \
            '/../../../../../test-data/images/set01/toucan.jpg'


class SimHashTests(unittest.TestCase):

    def setUp(self):
        if not os.path.exists("/models"):
            path = "/../../../../../containers/zmlp-plugins-models/resnet-152"
            SimilarityModel.path = os.path.dirname(__file__) + path

    def test_get_similarity_hash(self):

        SimilarityModel.load()
        with open(test_file, 'rb') as fp:
            simhash = get_similarity_hash(fp)
        assert simhash.startswith('PBPCLPPIAPPPGNKMPPPKMOPI')
