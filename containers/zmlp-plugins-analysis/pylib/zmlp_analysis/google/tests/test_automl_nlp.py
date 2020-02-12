#!/usr/bin/env python

import os

from zmlpsdk.testing import TestAsset, PluginUnitTestCase, zorroa_test_data
from zmlpsdk import Frame
from ..automl import AutoMLNLPModelProcessor


class AutoMLNLPUnitTests(PluginUnitTestCase):

    # Test disabled until Travis or GitLab have latest version of zorroa-test-data
    def do_not_test_model_eval(self):
        self.processor = AutoMLNLPModelProcessor()
        args = {
            'project_id': 'zorroa-poc-dev',
            'location_id': 'us-central1',
            'model_id': 'TCN1053548589589516491',
            'src_field': 'analysis.google.documentTextDetection.content',
            'label_map': {'Dienstalter_Anwartschaft': 'di_an'},
            'gcp_credentials_path': os.path.join(zorroa_test_data(), 'creds',
                                                 'zorroa-poc-dev-access.json'),
        }
        self.init_processor(self.processor, args)

        frame = zorroa.zsdk.Frame(zorroa.zsdk.Document()) # noqa
        frame.asset.set_attr('analysis.google.documentTextDetection.content',
                             "Dazu erteile ich Ihnen meine Zustimmung zur Gehaltsumwandlung in "
                             "H\xc3\xb6he meines\njeweiligen Finanzierungsanteils "
                             "gem\xc3\xa4\xc3\x9f Leistungsplan A am Gesamtbeitrag an die")
        self.processor.process(frame)
        scores = frame.asset.get_attr('analysis.google.automl_nlp.coba_01')
        self.processor.logger.info(scores)

        # Check the number of keys
        self.assertEqual(len(scores.keys()), 7)

        # Check the number of keys with a score greater than 0.5
        better_than_half_count = 0
        for k in scores.keys():
            if scores[k] > 0.5:
                better_than_half_count += 1
        self.assertEqual(better_than_half_count, 1)

        # Check that the key with the score greater than 0.5 is "other"
        self.assertAlmostEqual(scores['other'], 0.949701726436615)

        # Check one low key just for fun
        self.assertAlmostEqual(scores['bvv'], 0.018926216289401054)

        # Check for mapping success
        self.assertNotIn('dienstalter_anwartschaft', scores)
        self.assertIn('di_an', scores)
        self.assertAlmostEqual(scores['di_an'], 0.011406621895730495)

    def do_not_test_multipage(self):
        # TODO: How archivist.AssetSearch() doesn't work in test environments.
        # How to test?

        self.processor = AutoMLNLPModelProcessor()
        args = {
            'project_id': 'zorroa-poc-dev',
            'location_id': 'us-central1',
            'model_id': 'TCN1053548589589516491',
            'src_field': 'analysis.google.documentTextDetection.content',
            'collapse_multipage': True,
            'ignore_pages': [1, 4],
            'gcp_credentials_path': os.path.join(zorroa_test_data(), 'creds',
                                                 'zorroa-poc-dev-access.json'),
        }
        self.init_processor(self.processor, args)

        frame_0 = Frame(TestAsset())
        frame_0.asset.id = 'f0'
        frame_0.asset.set_attr('media.pages', 5)

        frame_1 = Frame(TestAsset())
        frame_1.asset.id = 'f1'
        frame_1.asset.set_attr('media.clip.parent', frame_0.asset.id)
        frame_1.asset.set_attr('analysis.google.documentTextDetection.content', 'A')

        frame_2 = Frame(TestAsset())
        frame_2.asset.id = 'f2'
        frame_2.asset.set_attr('media.clip.parent', frame_0.asset.id)
        frame_2.asset.set_attr('analysis.google.documentTextDetection.content', 'B')

        frame_3 = Frame(TestAsset())
        frame_3.asset.id = 'f3'
        frame_3.asset.set_attr('media.clip.parent', frame_0.asset.id)
        frame_3.asset.set_attr('analysis.google.documentTextDetection.content', 'C')

        frame_4 = Frame(TestAsset())
        frame_4.asset.id = 'f4'
        frame_4.asset.set_attr('media.clip.parent', frame_0.asset.id)
        frame_4.asset.set_attr('analysis.google.documentTextDetection.content', 'D')

        frame_5 = Frame(TestAsset())
        frame_5.asset.id = 'f5'
        frame_5.asset.set_attr('media.clip.parent', frame_0.asset.id)
        frame_5.asset.set_attr('analysis.google.documentTextDetection.content', 'E')

        self.processor.process(frame_5)
        scores = frame_5.asset.get_attr('analysis.google.automl_nlp.results')
        self.assertIsNone(scores)

        self.processor.process(frame_0)
        scores = frame_0.asset.get_attr('analysis.google.automl_nlp.results')
