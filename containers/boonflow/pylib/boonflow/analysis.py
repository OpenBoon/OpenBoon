
__all__ = [
    'Prediction',
    'LabelDetectionAnalysis',
    'ContentDetectionAnalysis'
]


class Prediction:
    """
    A single ML prediction which includes at minimum a label
    and a score.

    """
    precision = 3

    def __init__(self, label, score, **kwargs):
        """
        Create a new Prediction instance.
        Args:
            label (str): The string label.
            score (float): The score/confidence.
        """
        if score > 1:
            raise ValueError(f'The prediction score must be a float between 0 and 1. ({score})')
        self.label = label
        self.score = round(float(score), self.precision)
        self.occurrences = 1
        self.attrs = dict(kwargs)

    def add_occurrence(self, score):
        """
        Add an occurrence of this prediction. This will increment
        'occurrences' by one.  Additionally if the current score is
        less than the given score it is replaced.

        Args:
            score (float): the score for the new occurrence.

        """
        if self.occurrences is not None:
            self.occurrences += 1
        score = round(float(score), self.precision)
        if score > self.score:
            self.score = score

    def set_attr(self, key, value):
        """
        Set an arbitrary attribute on the prediction.  See the docs for
        common attributes. Invalid attributes may be rejected.

        Args:
            key (str): The attribute name.
            value (mixed): The value of the attribute.

        Returns:

        """
        self.attrs[key] = value

    def for_json(self, save_attrs=True):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Args:
            save_attrs (bool): Save arbitrary prediction attrs.  Not
                good for video.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        base = {
            "label": self.label,
            "score": self.score,
        }
        if save_attrs:
            base.update(self.attrs)
        # If occurrences is nulled out, don't include.
        if self.occurrences is not None:
            base["occurrences"] = self.occurrences
        return base

    def __eq__(self, other):
        return other.label == self.label

    def __hash__(self):
        return hash(self.label)

    def __str__(self):
        return '<Prediction label="{}">'.format(self.label)


class LabelDetectionAnalysis:
    """
    LabelDetectionSchema maintains a unique set of Predictions.  Note that the
    internals of this class are not oriented the same way the data is
    represented on the Asset.  The 'for_json' transforms the data into something
    more suitable for ElasticSearch.

    """
    MAX_PREDICTIONS = 32
    """maximum number of predictions for this analysis"""

    def __init__(self,
                 min_score=0.0,
                 max_predictions=None,
                 collapse_labels=False,
                 save_pred_attrs=True):
        """
        Create a new LabelDetectionSchema instance.

        Args:
            min_score (float): The minimum score a prediction must have to be included.
            max_predictions (int): The max number of predictions.
            collapse_labels (bool): If true, labels of the same name are collapsed into single
                entry with an occurrence count. This is desired fo video. Default its false.
            save_pred_attrs (bool): Serialize arbitrary prediction attrs, not good for video.

        """
        self.min_score = min_score
        self.max_predictions = max_predictions or self.MAX_PREDICTIONS
        self.collapse_labels = collapse_labels
        self.save_pred_attrs = save_pred_attrs

        self.pred_map = {}
        self.pred_list = []
        self.attrs = {}

    def set_attr(self, key, value):
        """
        Set an arbitrary attr on the LabelDetectionSchema.  See the docs for
        common attributes.  Invalid attributes may be rejected.

        Args:
            key (str): the name of the key.
            value (mixed): the value.

        """
        self.attrs[key] = value

    def add_label_and_score(self, label, score, **kwargs):
        """
        A convenience methods for adding a Prediction.
        Args:
            label (str): The label name.
            score (float): The score/confidence.
            **kwargs: Arbitrary key/value pairs added to the predication.
        Returns:
            bool: True if prediction was added. False if the score was not high enough.
        """
        return self.add_prediction(Prediction(label, score, **kwargs))

    def add_predictions(self, predictions):
        """
        Add a list of predictiond to Analysis.

        Args:
            predictions (list): A list of predictions.
        """
        for pred in predictions:
            self.add_prediction(pred)

    def add_prediction(self, pred):
        """
        Add a label prediction to this schema.  If label collapsing is enabled
        and a label with the same name is added it will not be added again.  However
        the confidence value will be updated if the new prediction has a higher
        confidence value.

        Args:
            pred (Prediction):

        Returns:
            bool: True if prediction was added. False if the score was not high enough.
        """
        if pred.score < self.min_score:
            return False

        if self.collapse_labels:
            existing = self.pred_map.get(pred.label)
            if not existing:
                self.pred_map[pred.label] = pred
            else:
                existing.add_occurrence(pred.score)
            return True
        else:
            self.pred_list.append(pred)
            pred.occurrences = None
            return True

    def predictions_list(self):
        """
        Return a list of all labels.

        Returns:
            list[str]: A list of labels.
        """

        if self.collapse_labels:
            base_list = [p for p in self.pred_map.values()]
        else:
            base_list = self.pred_list

        return sorted(base_list, key=lambda o: o.score, reverse=True)

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.
        """
        predictions = self.predictions_list()
        predict_count = len(predictions)
        base = {
            'type': 'labels',
            'count': min(predict_count, self.max_predictions),
            'predictions': [p.for_json(self.save_pred_attrs)
                            for p in predictions[0: min(predict_count, self.max_predictions)]]
        }
        base.update(self.attrs)
        return base

    def __len__(self):
        if self.collapse_labels:
            return len(self.pred_map)
        else:
            return len(self.pred_list)

    def __bool__(self):
        return len(self) > 0


class ContentDetectionAnalysis:
    """
    ContentDetectionAnalysis stores a blob of text content, for the
    results of an OCR process.

    """
    def __init__(self, unique_words=False, **kwargs):
        """
        Create a new ContentDetectionAnalysis instance.

        Args:
            unique_words (bool): Set to true if words should be unique.

        """
        self.unique_words = unique_words
        self.content = []
        self.attrs = dict(kwargs)

    def set_attr(self, key, value):
        """
        Set an arbitrary attr on the LabelDetectionSchema.  See the docs for
        common attributes.  Invalid attributes may be rejected.

        Args:
            key (str): the name of the key.
            value (mixed): the value.

        """
        self.attrs[key] = value

    def add_content(self, val):
        """
        Add content to the analysis.

        Args:
            val (str): The string containing the content.

        """
        val = val.replace('\r', ' ').replace('\n', ' ')
        self.content.append(val)

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.
        """
        text = ' '.join(self.content)
        words = text.split()

        # this basic processing removes line breaks, etc.
        if self.unique_words:
            words = frozenset(words)
            # Words are only sorted if unqiue.
            text = ' '.join(sorted(words))
        else:
            text = ' '.join(words)

        if len(text) > 32766:
            text = text[:32765]

        base = {
            'type': 'content',
            'words': len(words),
            'content': text
        }
        base.update(self.attrs)
        return base

    def __bool__(self):
        return len(self.content) > 0
