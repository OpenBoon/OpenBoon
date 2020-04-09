
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

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        base = {
            "label": self.label,
            "score": self.score,
            "occurrences": self.occurrences,
        }
        base.update(self.attrs)
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
    def __init__(self, min_score=0.15, max_predictions=128):
        """
        Create a new LabelDetectionSchema instance.

        Args:
            min_score (float): The minimum score a prediction must have to be included.
            max_predictions (int): The max number of predictions.

        """
        self.min_score = min_score
        self.max_predictions = max_predictions
        self.predictions = {}
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

    def add_prediction(self, pred):
        """
        Add a label prediction to this schema.

        Args:
            pred (Prediction):

        Returns:
            bool: True if prediction was added. False if the score was not high enough.
        """
        if pred.score < self.min_score:
            return False
        existing = self.predictions.get(pred.label)
        if not existing:
            self.predictions[pred.label] = pred
        else:
            existing.add_occurrence(pred.score)
        return True

    def predictions_list(self):
        """
        Return a list of all labels.

        Returns:
            list[str]: A list of labels.
        """
        return sorted([p for p in self.predictions.values()], key=lambda o: o.score, reverse=True)

    def predictions_map(self):
        """
        Return a dictionary of label to confidence.

        Returns:
            dict[str,float] - A dict of labels and confidence.
        """
        return dict([(p.label, p.score) for p in self.predictions.values()])

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
            'predictions': predictions[0: min(predict_count, self.max_predictions)]
        }
        base.update(self.attrs)
        return base


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
