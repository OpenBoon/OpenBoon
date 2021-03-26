import json

import requests
from requests import Response
from rest_framework.reverse import reverse


def test_usage_report_view(login, api_client, project, monkeypatch):
    def mock_metrics_response(*args, **kwargs):
        data = [{'image_count': 1,
                 'project': '00000000-0000-0000-0000-000000000000',
                 'service': 'boonai-object-detection',
                 'tier': 'tier_1',
                 'video_minutes': 7.17}]
        _response = Response()
        _response.status_code = 200
        _response._content = json.dumps(data).encode('utf-8')
        return _response

    monkeypatch.setattr(requests, 'get', mock_metrics_response)
    path = reverse('generate-usage-report')
    response = api_client.post(path)
    assert response.status_code == 200
    assert response.get('content-type') == 'text/csv'
