import cv2
import numpy
import pytest

from unittest.mock import patch

from models.utils import ConfusionMatrix
from boonsdk import Model


def mock_aggs(*args, **kwargs):
    return {
        'nested#nested_labels': {
            'doc_count': 1744,
            'filter#model_train_labels': {
                'doc_count': 838,
                'sterms#labels': {
                    'doc_count_error_upper_bound': 0,
                    'sum_other_doc_count': 0,
                    'buckets': [{'key': 'bird',
                                 'doc_count': 90,
                                 'reverse_nested#predictions_by_label': {
                                     'doc_count': 90,
                                     'sterms#predictions': {
                                         'doc_count_error_upper_bound': 0,
                                         'sum_other_doc_count': 0,
                                         'buckets': [
                                             {
                                                 'key': 'Unrecognized',
                                                 'doc_count': 60},
                                             {
                                                 'key': 'bird',
                                                 'doc_count': 28},
                                             {
                                                 'key': 'frog',
                                                 'doc_count': 2}]}}},
                                {'key': 'deer',
                                 'doc_count': 90,
                                 'reverse_nested#predictions_by_label': {
                                     'doc_count': 90,
                                     'sterms#predictions': {
                                         'doc_count_error_upper_bound': 0,
                                         'sum_other_doc_count': 0,
                                         'buckets': [
                                             {
                                                 'key': 'deer',
                                                 'doc_count': 88},
                                             {
                                                 'key': 'Unrecognized',
                                                 'doc_count': 2}]}}},
                                {'key': 'dog',
                                 'doc_count': 90,
                                 'reverse_nested#predictions_by_label': {
                                     'doc_count': 90,
                                     'sterms#predictions': {
                                         'doc_count_error_upper_bound': 0,
                                         'sum_other_doc_count': 0,
                                         'buckets': [
                                             {
                                                 'key': 'dog',
                                                 'doc_count': 64},
                                             {
                                                 'key': 'Unrecognized',
                                                 'doc_count': 20},
                                             {
                                                 'key': 'cat',
                                                 'doc_count': 3},
                                             {
                                                 'key': 'horse',
                                                 'doc_count': 2},
                                             {
                                                 'key': 'deer',
                                                 'doc_count': 1}]}}},
                                {'key': 'frog',
                                 'doc_count': 87,
                                 'reverse_nested#predictions_by_label': {
                                     'doc_count': 87,
                                     'sterms#predictions': {
                                         'doc_count_error_upper_bound': 0,
                                         'sum_other_doc_count': 0,
                                         'buckets': [
                                             {
                                                 'key': 'frog',
                                                 'doc_count': 83},
                                             {
                                                 'key': 'Unrecognized',
                                                 'doc_count': 4}]}}},
                                {'key': 'cat',
                                 'doc_count': 86,
                                 'reverse_nested#predictions_by_label': {
                                     'doc_count': 86,
                                     'sterms#predictions': {
                                         'doc_count_error_upper_bound': 0,
                                         'sum_other_doc_count': 0,
                                         'buckets': [
                                             {
                                                 'key': 'cat',
                                                 'doc_count': 83},
                                             {
                                                 'key': 'Unrecognized',
                                                 'doc_count': 3}]}}},
                                {'key': 'ship',
                                 'doc_count': 84,
                                 'reverse_nested#predictions_by_label': {
                                     'doc_count': 84,
                                     'sterms#predictions': {
                                         'doc_count_error_upper_bound': 0,
                                         'sum_other_doc_count': 0,
                                         'buckets': [
                                             {
                                                 'key': 'ship',
                                                 'doc_count': 70},
                                             {
                                                 'key': 'Unrecognized',
                                                 'doc_count': 14}]}}},
                                {'key': 'horse',
                                 'doc_count': 82,
                                 'reverse_nested#predictions_by_label': {
                                     'doc_count': 82,
                                     'sterms#predictions': {
                                         'doc_count_error_upper_bound': 0,
                                         'sum_other_doc_count': 0,
                                         'buckets': [
                                             {
                                                 'key': 'horse',
                                                 'doc_count': 75},
                                             {
                                                 'key': 'Unrecognized',
                                                 'doc_count': 7}]}}},
                                {'key': 'truck',
                                 'doc_count': 82,
                                 'reverse_nested#predictions_by_label': {
                                     'doc_count': 82,
                                     'sterms#predictions': {
                                         'doc_count_error_upper_bound': 0,
                                         'sum_other_doc_count': 0,
                                         'buckets': [
                                             {
                                                 'key': 'truck',
                                                 'doc_count': 63},
                                             {
                                                 'key': 'Unrecognized',
                                                 'doc_count': 19}]}}},
                                {
                                    'key': 'airplane',
                                    'doc_count': 75,
                                    'reverse_nested#predictions_by_label': {
                                        'doc_count': 75,
                                        'sterms#predictions': {
                                            'doc_count_error_upper_bound': 0,
                                            'sum_other_doc_count': 0,
                                            'buckets': [
                                                {
                                                    'key': 'airplane',
                                                    'doc_count': 61},
                                                {
                                                    'key': 'Unrecognized',
                                                    'doc_count': 14}]}}},
                                {
                                    'key': 'automobile',
                                    'doc_count': 72,
                                    'reverse_nested#predictions_by_label': {
                                        'doc_count': 72,
                                        'sterms#predictions': {
                                            'doc_count_error_upper_bound': 0,
                                            'sum_other_doc_count': 0,
                                            'buckets': [
                                                {
                                                    'key': 'Unrecognized',
                                                    'doc_count': 38},
                                                {
                                                    'key': 'truck',
                                                    'doc_count': 24},
                                                {
                                                    'key': 'automobile',
                                                    'doc_count': 9},
                                                {
                                                    'key': 'frog',
                                                    'doc_count':
                                                        1}]}}}]}}}}


def test_get_confusion_matrix(monkeypatch):
    monkeypatch.setattr(ConfusionMatrix, '_ConfusionMatrix__get_confusion_matrix_aggregations',
                        mock_aggs)
    matrix = ConfusionMatrix(Model({'name': 'test', 'moduleName': 'also-test'}), None)
    assert matrix.labels == ['Unrecognized', 'airplane', 'automobile', 'bird', 'cat',
                             'deer', 'dog', 'frog', 'horse', 'ship', 'truck']
    assert matrix.accuracy == 0.7446300715990454
    assert matrix.test_set_only
    assert matrix.min_score == 0.0
    assert matrix.max_score == 1.0
    assert matrix.to_dict() == {'labels': ['Unrecognized',
                                           'airplane',
                                           'automobile',
                                           'bird',
                                           'cat',
                                           'deer',
                                           'dog',
                                           'frog',
                                           'horse',
                                           'ship',
                                           'truck'],
                                'matrix': [[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                                           [14, 61, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                                           [38, 0, 9, 0, 0, 0, 0, 1, 0, 0, 24],
                                           [60, 0, 0, 28, 0, 0, 0, 2, 0, 0, 0],
                                           [3, 0, 0, 0, 83, 0, 0, 0, 0, 0, 0],
                                           [2, 0, 0, 0, 0, 88, 0, 0, 0, 0, 0],
                                           [20, 0, 0, 0, 3, 1, 64, 0, 2, 0, 0],
                                           [4, 0, 0, 0, 0, 0, 0, 83, 0, 0, 0],
                                           [7, 0, 0, 0, 0, 0, 0, 0, 75, 0, 0],
                                           [14, 0, 0, 0, 0, 0, 0, 0, 0, 70, 0],
                                           [19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 63]],
                                'maxScore': 1.0,
                                'minScore': 0.0,
                                'name': 'test',
                                'moduleName': 'also-test',
                                'overallAccuracy': 0.7446300715990454,
                                'testSetOnly': True,
                                'isMatrixApplicable': True}
    assert matrix.to_dict(normalize_matrix=True) == {'labels': ['Unrecognized',
                                                                'airplane',
                                                                'automobile',
                                                                'bird',
                                                                'cat',
                                                                'deer',
                                                                'dog',
                                                                'frog',
                                                                'horse',
                                                                'ship',
                                                                'truck'],
                                                     'matrix': [
                                                         [0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                                                          0.0, 0.0, 0.0, 0.0, 0.0],
                                                         [0.18666666666666668,
                                                          0.8133333333333334, 0.0, 0.0,
                                                          0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                                                          0.0],
                                                         [0.5277777777777778,
                                                          0.0,
                                                          0.125,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.013888888888888888,
                                                          0.0,
                                                          0.0,
                                                          0.3333333333333333],
                                                         [0.6666666666666666,
                                                          0.0,
                                                          0.0,
                                                          0.3111111111111111,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.022222222222222223,
                                                          0.0,
                                                          0.0,
                                                          0.0],
                                                         [0.03488372093023256,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.9651162790697675,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0],
                                                         [0.022222222222222223,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.9777777777777777,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0],
                                                         [0.2222222222222222,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.03333333333333333,
                                                          0.011111111111111112,
                                                          0.7111111111111111,
                                                          0.0,
                                                          0.022222222222222223,
                                                          0.0,
                                                          0.0],
                                                         [0.04597701149425287,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.9540229885057471,
                                                          0.0,
                                                          0.0,
                                                          0.0],
                                                         [0.08536585365853659,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.9146341463414634,
                                                          0.0,
                                                          0.0],
                                                         [0.16666666666666666,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.8333333333333334,
                                                          0.0],
                                                         [0.23170731707317074,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.0,
                                                          0.7682926829268293]],
                                                     'maxScore': 1.0,
                                                     'minScore': 0.0,
                                                     'name': 'test',
                                                     'moduleName': 'also-test',
                                                     'overallAccuracy': 0.7446300715990454,
                                                     'testSetOnly': True,
                                                     'isMatrixApplicable': True}
    thumbnail = ConfusionMatrix(Model({'name': 'test'}), None).create_thumbnail_image()
    npimg = numpy.fromstring(thumbnail.read(), numpy.uint8)
    image = cv2.imdecode(npimg, cv2.IMREAD_UNCHANGED)
    assert image.shape == (500, 500, 3)


def test_empty_confusion_matrix(monkeypatch):
    def mock_aggs(*args, **kwargs):
        return {
            'nested#nested_labels': {
                'doc_count': 0,
                'filter#model_train_labels': {'doc_count': 0,
                                              'sterms#labels': {
                                                  'doc_count_error_upper_bound': 0,
                                                  'sum_other_doc_count': 0,
                                                  'buckets': []}}}}
    monkeypatch.setattr(ConfusionMatrix,
                        '_ConfusionMatrix__get_confusion_matrix_aggregations',
                        mock_aggs)
    matrix = ConfusionMatrix(Model({'name': 'test'}), None)
    assert matrix.accuracy == 0.0


def full_return():
    return {'nested#nested_labels': {'doc_count': 1894, 'filter#model_train_labels': {'doc_count': 835, 'sterms#labels': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'bird', 'doc_count': 90, 'reverse_nested#predictions_by_label': {'doc_count': 90, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'Unrecognized', 'doc_count': 56}, {'key': 'bird', 'doc_count': 27}, {'key': 'frog', 'doc_count': 4}, {'key': 'horse', 'doc_count': 3}]}}}, {'key': 'deer', 'doc_count': 90, 'reverse_nested#predictions_by_label': {'doc_count': 90, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'deer', 'doc_count': 88}, {'key': 'Unrecognized', 'doc_count': 2}]}}}, {'key': 'dog', 'doc_count': 89, 'reverse_nested#predictions_by_label': {'doc_count': 89, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'dog', 'doc_count': 78}, {'key': 'Unrecognized', 'doc_count': 4}, {'key': 'horse', 'doc_count': 3}, {'key': 'cat', 'doc_count': 2}, {'key': 'deer', 'doc_count': 1}, {'key': 'truck', 'doc_count': 1}]}}}, {'key': 'frog', 'doc_count': 87, 'reverse_nested#predictions_by_label': {'doc_count': 87, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'frog', 'doc_count': 87}]}}}, {'key': 'cat', 'doc_count': 86, 'reverse_nested#predictions_by_label': {'doc_count': 86, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'cat', 'doc_count': 83}, {'key': 'Unrecognized', 'doc_count': 3}]}}}, {'key': 'ship', 'doc_count': 84, 'reverse_nested#predictions_by_label': {'doc_count': 84, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'ship', 'doc_count': 68}, {'key': 'Unrecognized', 'doc_count': 9}, {'key': 'truck', 'doc_count': 7}]}}}, {'key': 'truck', 'doc_count': 82, 'reverse_nested#predictions_by_label': {'doc_count': 82, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'truck', 'doc_count': 74}, {'key': 'Unrecognized', 'doc_count': 8}]}}}, {'key': 'horse', 'doc_count': 80, 'reverse_nested#predictions_by_label': {'doc_count': 80, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'horse', 'doc_count': 78}, {'key': 'Unrecognized', 'doc_count': 2}]}}}, {'key': 'airplane', 'doc_count': 75, 'reverse_nested#predictions_by_label': {'doc_count': 75, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'airplane', 'doc_count': 61}, {'key': 'Unrecognized', 'doc_count': 13}, {'key': 'truck', 'doc_count': 1}]}}}, {'key': 'automobile', 'doc_count': 72, 'reverse_nested#predictions_by_label': {'doc_count': 72, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'truck', 'doc_count': 47}, {'key': 'Unrecognized', 'doc_count': 16}, {'key': 'automobile', 'doc_count': 9}]}}}]}}}}  # noqa


def filtered_return():
    return {'nested#nested_labels': {'doc_count': 125, 'filter#model_train_labels': {'doc_count': 24, 'sterms#labels': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'ship', 'doc_count': 22, 'reverse_nested#predictions_by_label': {'doc_count': 22, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'ship', 'doc_count': 22}]}}}, {'key': 'automobile', 'doc_count': 1, 'reverse_nested#predictions_by_label': {'doc_count': 1, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'automobile', 'doc_count': 1}]}}}, {'key': 'frog', 'doc_count': 1, 'reverse_nested#predictions_by_label': {'doc_count': 1, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'frog', 'doc_count': 1}]}}}]}}}}  # noqa


def test_get_dict_from_agg_results():
    matrix = ConfusionMatrix(Model({'name': 'test', 'moduleName': 'also-test'}), None,
                             min_score=0.5, max_score=1.0)
    results = matrix._ConfusionMatrix__get_dict_from_agg_results(full_return())
    assert results == {
        'airplane': {'Unrecognized': 13, 'airplane': 61, 'truck': 1},
        'automobile': {'Unrecognized': 16, 'automobile': 9, 'truck': 47},
        'bird': {'Unrecognized': 56, 'bird': 27, 'frog': 4, 'horse': 3},
        'cat': {'Unrecognized': 3, 'cat': 83},
        'deer': {'Unrecognized': 2, 'deer': 88},
        'dog': {'Unrecognized': 4, 'cat': 2, 'deer': 1, 'dog': 78, 'horse': 3, 'truck': 1},
        'frog': {'frog': 87},
        'horse': {'Unrecognized': 2, 'horse': 78},
        'ship': {'Unrecognized': 9, 'ship': 68, 'truck': 7},
        'truck': {'Unrecognized': 8, 'truck': 74}}


@patch.object(ConfusionMatrix, '_ConfusionMatrix__get_confusion_matrix_aggregations')
def test_filtered_matrix_returns_all_labels(_mock):
    _mock.side_effect = [full_return(), filtered_return()]
    matrix = ConfusionMatrix(Model({'name': 'test', 'moduleName': 'also-test'}), None,
                             min_score=0.5, max_score=1.0)
    assert matrix.labels == ['Unrecognized', 'airplane', 'automobile', 'bird', 'cat',
                             'deer', 'dog', 'frog', 'horse', 'ship', 'truck']
    assert matrix.to_dict() == {'labels': ['Unrecognized',
                                           'airplane',
                                           'automobile',
                                           'bird',
                                           'cat',
                                           'deer',
                                           'dog',
                                           'frog',
                                           'horse',
                                           'ship',
                                           'truck'],
                                'matrix': [[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                                           [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                                           [0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0],
                                           [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                                           [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                                           [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                                           [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                                           [0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0],
                                           [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                                           [0, 0, 0, 0, 0, 0, 0, 0, 0, 22, 0],
                                           [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]],
                                'maxScore': 1.0,
                                'minScore': 0.5,
                                'name': 'test',
                                'moduleName': 'also-test',
                                'overallAccuracy': 1.0,
                                'testSetOnly': True,
                                'isMatrixApplicable': True}


def test_confusion_matrix_no_dataset():
    matrix = ConfusionMatrix(Model({'name': 'test', 'moduleName': 'also-test'}),
                             None, min_score=0.5, max_score=1.0)
    with pytest.raises(ValueError):
        matrix.to_dict()
