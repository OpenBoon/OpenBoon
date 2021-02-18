from unittest import TestCase

from boonai_analysis.utils.simengine import SimilarityEngine
from boonflow.testing import test_path


class SimilarityEngineTests(TestCase):

    def test_calculate_hash(self):
        eng = SimilarityEngine(test_path("models/resnet-152"))
        sim = eng.calculate_hash(test_path("images/set01/faces.jpg"))
        assert sim.startswith("PPPPGCBIPPK")
        assert len(sim) == 2048
