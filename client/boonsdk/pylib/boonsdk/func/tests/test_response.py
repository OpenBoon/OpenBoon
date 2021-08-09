import json
from unittest import TestCase

import boonsdk.func as analysis
from boonsdk.client import to_json


class PredicationTests(TestCase):

    def setUp(self):
        self.pred = analysis.Prediction('cat', 0.50)

    def test_create(self):
        pred = analysis.Prediction('dog', 0.15, simhash="abc", tags=['brown'])
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


class LabelDetectionAnalysisTestsCollapsed(TestCase):

    def setUp(self):
        self.analysis = analysis.LabelDetectionAnalysis(min_score=0.15, collapse_labels=True)
        self.pred = analysis.Prediction('cat', 0.50)

    def test_add_prediction(self):
        assert self.analysis.add_prediction(self.pred) is True
        assert self.analysis.add_prediction(self.pred) is True
        assert self.analysis.add_prediction(analysis.Prediction('dog', 0.01)) is False
        assert 1 == len(self.analysis)

    def test_add_predictions(self):
        preds = [analysis.Prediction('cat', 0.50), analysis.Prediction('dog', 0.9)]
        self.analysis.add_predictions(preds)
        assert 2 == len(self.analysis)

    def test_add_label_and_score(self):
        assert self.analysis.add_label_and_score("dog", 0.5, color='brown') is True
        assert self.analysis.add_label_and_score("dog", 0.6) is True
        assert 1 == len(self.analysis)
        pred = self.analysis.pred_map['dog']
        assert 'dog' == pred.label
        assert 0.6 == pred.score
        assert 'brown' == pred.attrs['color']

    def test_max_predictions(self):
        self.analysis.max_predictions = 1
        self.analysis.add_prediction(self.pred)
        self.analysis.add_prediction(analysis.Prediction('dog', 0.54))
        assert 2 == len(self.analysis)

        serialized = json.loads(to_json(self.analysis))
        assert 1 == len(serialized['predictions'])
        assert 'dog' == serialized['predictions'][0]['label']


class LabelDetectionAnalysisTests(TestCase):

    def setUp(self):
        self.analysis = analysis.LabelDetectionAnalysis(min_score=0.15, collapse_labels=False)
        self.pred = analysis.Prediction('cat', 0.50)

    def test_add_predictions_no_attrs(self):
        labels = analysis.LabelDetectionAnalysis(save_pred_attrs=False)
        preds = [analysis.Prediction('dog', 0.9, bbox=[1, 2, 3, 4])]
        labels.add_predictions(preds)
        sd = labels.for_json()
        assert not sd['predictions'][0].get('bbox')

    def test_add_predictions_with_attrs(self):
        labels = analysis.LabelDetectionAnalysis()
        preds = [analysis.Prediction('dog', 0.9, bbox=[1, 2, 3, 4])]
        labels.add_predictions(preds)
        sd = labels.for_json()
        assert sd['predictions'][0].get('bbox')

    def test_add_prediction(self):
        assert self.analysis.add_prediction(self.pred) is True
        assert self.analysis.add_prediction(self.pred) is True
        assert self.analysis.add_prediction(analysis.Prediction('dog', 0.01)) is False
        assert 2 == len(self.analysis)

    def test_add_label_and_score(self):
        assert self.analysis.add_label_and_score("dog", 0.5, color='brown') is True
        assert self.analysis.add_label_and_score("dog", 0.6) is True
        assert 2 == len(self.analysis)
        pred = self.analysis.pred_list[0]
        assert 'dog' == pred.label
        assert 0.5 == pred.score
        assert 'brown' == pred.attrs['color']

    def test_max_predictions(self):
        self.analysis.max_predictions = 1
        self.analysis.add_prediction(self.pred)
        self.analysis.add_prediction(analysis.Prediction('dog', 0.54))
        assert 2 == len(self.analysis)

        serialized = json.loads(to_json(self.analysis))
        assert 1 == len(serialized['predictions'])
        assert 'dog' == serialized['predictions'][0]['label']


class ContentDetectionAnalysisTests(TestCase):

    def setUp(self):
        self.analysis = analysis.ContentDetectionAnalysis(lang="us")

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
        predictions = analysis.ContentDetectionAnalysis(unique_words=True, lang="us")
        text1 = 'dog cat dog cat mouse'
        text2 = 'mouse dog cat dog'
        predictions.add_content(text1)
        predictions.add_content(text2)

        serialized = json.loads(to_json(predictions))
        print(serialized)
        assert 3 == serialized['words']
        assert "us" == serialized['lang']
        assert "cat dog mouse" == serialized['content']
