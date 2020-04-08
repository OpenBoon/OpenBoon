import os
import unittest

from mlbbq.simhash import get_similarity_hash, SimilarityModel

test_file = os.path.dirname(__file__) + \
            '/../../../../../test-data/images/set01/toucan.jpg'

SimilarityModel.path = os.path.dirname(__file__) + \
                       "/../../../../../containers/zmlp-plugins-models/resnet-152"

class SimHashTests(unittest.TestCase):

    def test_get_similarity_hash(self):

        SimilarityModel.load()
        with open(test_file, 'rb') as fp:
            simhash = get_similarity_hash(fp)
        assert simhash.startswith('PBPCLPPIAPPPGNKMPPPKMOPI')

