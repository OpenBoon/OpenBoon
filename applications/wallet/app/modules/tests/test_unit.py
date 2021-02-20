import pytest

from modules.serializers import ModuleSerializer


@pytest.fixture
def data():
    return {'page': {'from': 0, 'size': 50, 'disabled': False, 'totalCount': 2},
            'list': [{'id': 'bf5689d4-6a0d-11ea-a36b-0242ac12000b',
                      'name': 'clarifai-predict',
                      'description': 'Clarifai prediction API, standard model.',
                      'restricted': False,
                      'ops': [{'type': 'APPEND',
                               'apply': [{'args': {},
                                          'image': 'zmlp/plugins-analysis',
                                          'module': 'standard',
                                          'checksum': 3907589052,
                                          'className': 'boonai_analysis.clarifai.ClarifaiPredictProcessor'}],  # noqa
                               'maxApplyCount': 1}],
                      'timeCreated': 1584641786546,
                      'timeModified': 1584987295350,
                      'actorCreated': '00000000-1234-1234-1234-000000000000/background-thread',
                      'actorModified': '00000000-1234-1234-1234-000000000000/background-thread'},
                     {'id': 'bf58d3c5-6a0d-11ea-a36b-0242ac12000b',
                      'name': 'gcp-label-detection',
                      'description': 'Utilize Google Cloud Vision label detection to detect and extract information about entities in an image, across a broad group of categories.',  # noqa
                      'restricted': False,
                      'ops': [{'type': 'APPEND',
                               'apply': [{'args': {},
                                          'image': 'zmlp/plugins-analysis',
                                          'module': 'standard',
                                          'checksum': 2975666803,
                                          'className': 'boonai_analysis.google.CloudVisionDetectLabels'}],  # noqa
                               'maxApplyCount': 1}],
                      'timeCreated': 1584641786561,
                      'timeModified': 1584987295364,
                      'actorCreated': '00000000-1234-1234-1234-000000000000/background-thread',
                      'actorModified': '00000000-1234-1234-1234-000000000000/background-thread'}
                     ]
            }


class TestModuleSerializer():

    def test_basic_serialization(self, data):
        serializer = ModuleSerializer(data=data['list'], many=True)
        assert serializer.is_valid()
        result = serializer.data
        assert result[0]['name'] == 'clarifai-predict'
        assert len(result) == 2
