import os
import unittest

from mlbbq.simhash import get_similarity_hash

test_file = os.path.dirname(__file__) + \
            '/../../../../../test-data/images/set01/toucan.jpg'


class SimHashTests(unittest.TestCase):

    def test_get_similarity_hash(self):
        with open(test_file, 'rb') as fp:
            simhash = get_similarity_hash(fp)
        assert simhash.startswith('PBPCLPPIAPPPGNKMPPPKMOPI')

