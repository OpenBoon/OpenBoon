from unittest import TestCase

from boonai_analysis.utils.simengine import SimilarityEngine
from boonflow.testing import test_path


class SimilarityEngineTests(TestCase):

    simeng = SimilarityEngine(test_path("models/resnet-152"))

    def test_calculate_hash(self):
        sim = self.simeng.calculate_hash(test_path("images/set01/faces.jpg"))
        assert sim.startswith("PPPPGCBIPPK")
        assert len(sim) == 2048

    def test_hash_as_nparray(self):
        np = self.simeng.hash_as_nparray("AAAAAA")
        assert len(np) == 6
        assert np[0] == 65
