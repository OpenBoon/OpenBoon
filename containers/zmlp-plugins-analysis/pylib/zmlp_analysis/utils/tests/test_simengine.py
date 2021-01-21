from unittest import TestCase

from zmlp_analysis.utils.simengine import SimilarityEngine
from zmlpsdk.testing import zorroa_test_path


class SimilarityEngineTests(TestCase):

    def test_calculate_hash(self):
        eng = SimilarityEngine(zorroa_test_path("models/resnet-152"))
        sim = eng.calculate_hash(zorroa_test_path("images/set01/faces.jpg"))
        assert sim.startswith("PPPPGCBIPPK")
        assert len(sim) == 2048
