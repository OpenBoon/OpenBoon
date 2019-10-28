#!/usr/bin/env python
import unittest

from zorroa.zsdk.zpsgo import ProcessorStatsCollector


class StatsCollectorTests(unittest.TestCase):

    def test_constructor(self):
        """
        Test constructor.
        """
        collector = ProcessorStatsCollector()
        self.assertIsNotNone(collector.stats)
        self.assertEquals(0, len(collector.stats))

    def test_add(self):
        collector = ProcessorStatsCollector()
        processor_name = "test.processor.Name"
        processing_time = 0.1
        collector.add(processor_name, processing_time)
        self.assertIsNotNone(collector.stats.get(processor_name))
        self.assertEqual(1, len(collector.stats[processor_name]))
        self.assertEqual(processing_time, collector.stats[processor_name][0])

    def test_summary(self):
        collector = ProcessorStatsCollector()
        processor_name = "test.processor.Name"
        processing_time = 0.1
        collector.add(processor_name, processing_time)
        summary = collector.summary()
        expected = [{'avg': processing_time,
                     'count': 1,
                     'max': processing_time,
                     'min': processing_time,
                     'processor': processor_name}]
        self.assertEqual(expected, summary)
