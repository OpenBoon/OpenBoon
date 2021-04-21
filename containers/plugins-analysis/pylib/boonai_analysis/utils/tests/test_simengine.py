from unittest import TestCase

import cv2
import mxnet
from boonai_analysis.utils.simengine import SimilarityEngine
from boonflow.testing import test_path


class SimilarityEngineTests(TestCase):

    simeng = SimilarityEngine(test_path("models/resnet-152"))

    def test_calculate_hash_from_path(self):
        sim = self.simeng.calculate_simhash(test_path("images/set01/faces.jpg"))
        assert sim.startswith("PPPPGCBIPPK")
        assert len(sim) == 2048

    def test_calculate_hash_from_stream(self):
        with open(test_path("images/set01/faces.jpg"), "rb") as fp:
            sim = self.simeng.calculate_simhash(fp)
        assert sim.startswith("PPPPGCBIPPK")
        assert len(sim) == 2048

    def test_hash_as_nparray(self):
        np = self.simeng.hash_as_nparray("AAAAAA")
        assert len(np) == 6
        assert np[0] == 65

    def test_prep_cvimage(self):
        img = cv2.imread(test_path("images/set01/faces.jpg"))
        prepped = self.simeng.prep_cvimage(img)
        assert isinstance(prepped, mxnet.ndarray.ndarray.NDArray)
        assert len(prepped[0]) == 3
