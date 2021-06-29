import pytest

from rest_framework.test import APIClient
from metrics.records.models import ApiCall
from fixture_data.records import RECORDS


@pytest.fixture
def api_client():
    return APIClient()


@pytest.fixture
def single_record():
    return ApiCall.objects.create(project='92db38cc-a9d1-43af-bfb5-7be32de1d33d',
                                  service='TestService',
                                  asset_id='GTZ6ppbXYwXO4ssWYcPVaQJsXNC-cVap',
                                  asset_path='gs://bucket/image.jpg',
                                  image_count=1,
                                  video_seconds=0.0)


@pytest.fixture
def test_set():
    # Modified and Created dates are not used from the records since they're overridden
    # by the field properties
    for record in RECORDS:
        ApiCall.objects.create(id=record['id'],
                               project=record['project'],
                               service=record['service'],
                               asset_id=record['asset_id'],
                               asset_path=record['asset_path'],
                               image_count=record['image_count'],
                               video_seconds=record['video_seconds'])
