import json
from unittest import TestCase

from zmlp.client import to_json
from zmlpsdk import schema


class PredicationTests(TestCase):

    def setUp(self):
        self.pred = schema.Prediction('cat', 0.50)

    def test_create(self):
        pred = schema.Prediction('dog', 0.15, simhash="abc", tags=['brown'])
        serialized = pred.for_json()
        assert 'dog' == serialized['label']
        assert 0.15 == serialized['score']
        assert 1 == serialized['occurrences']
        assert 'abc' == serialized['simhash']
        assert ['brown'] == serialized['tags']

    def test_add_occurrence(self):
        self.pred.add_occurrence(0.40)
        assert self.pred.score == 0.50
        assert self.pred.occurrences == 2
        self.pred.add_occurrence(0.60)
        assert self.pred.score == 0.60
        assert self.pred.occurrences == 3

    def test_set_attr(self):
        self.pred.set_attr('simhash', 'ABCD')
        assert self.pred.attrs['simhash'] == 'ABCD'

    def test_for_json(self):
        self.pred.set_attr('simhash', 'ABCD')
        serialized = self.pred.for_json()
        assert 'cat' == serialized['label']
        assert 0.5 == serialized['score']
        assert 1 == serialized['occurrences']
        assert 'ABCD' == serialized['simhash']


class LabelDetectionAnalysisTests(TestCase):

    def setUp(self):
        self.analysis = schema.LabelDetectionAnalysis()
        self.pred = schema.Prediction('cat', 0.50)

    def test_add_prediction(self):
        assert self.analysis.add_prediction(self.pred) is True
        assert self.analysis.add_prediction(self.pred) is True
        assert self.analysis.add_prediction(schema.Prediction('dog', 0.01)) is False
        assert 1 == len(self.analysis.predictions)

    def test_add_label_and_score(self):
        assert self.analysis.add_label_and_score("dog", 0.5, color='brown') is True
        assert self.analysis.add_label_and_score("dog", 0.6) is True
        assert 1 == len(self.analysis.predictions)
        pred = self.analysis.predictions['dog']
        assert 'dog' == pred.label
        assert 0.6 == pred.score
        assert 'brown' == pred.attrs['color']

    def test_max_predictions(self):
        self.analysis.max_predictions = 1
        self.analysis.add_prediction(self.pred)
        self.analysis.add_prediction(schema.Prediction('dog', 0.54))
        assert 2 == len(self.analysis.predictions)

        serialized = json.loads(to_json(self.analysis))
        assert 1 == len(serialized['predictions'])
        assert 'dog' == serialized['predictions'][0]['label']


class ContentDetectionAnalysisTests(TestCase):

    def setUp(self):
        self.analysis = schema.ContentDetectionAnalysis(lang="us")

    def test_add_content(self):
        text = 'The dog ran'
        self.analysis.add_content(text)
        assert text in self.analysis.content

    def test_for_json(self):
        text = 'The dog ran'
        self.analysis.add_content(text)

        serialized = json.loads(to_json(self.analysis))
        assert 3 == serialized['words']
        assert text == serialized['content']
        assert "us" == serialized['lang']

    def test_for_json_unique(self):
        analysis = schema.ContentDetectionAnalysis(unique_words=True, lang="us")
        text1 = 'dog cat dog cat mouse'
        text2 = 'mouse dog cat dog'
        analysis.add_content(text1)
        analysis.add_content(text2)

        serialized = json.loads(to_json(analysis))
        print(serialized)
        assert 3 == serialized['words']
        assert "us" == serialized['lang']
        assert "cat dog mouse" == serialized['content']
