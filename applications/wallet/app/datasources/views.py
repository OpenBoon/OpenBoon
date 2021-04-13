import json
import os
import uuid
from functools import lru_cache

from boonsdk import DataSource
from boonsdk.client import BoonSdkDuplicateException
from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response

from datasources.serializers import (DataSourceSerializer, CreateDataSourceSerializer,
                                     AzureCredentialSerializer, AwsCredentialSerializer,
                                     GcpCredentialSerializer)
from projects.viewsets import (BaseProjectViewSet, ZmlpListMixin, ZmlpRetrieveMixin,
                               ZmlpDestroyMixin, ListViewType)
from wallet.utils import validate_zmlp_data


def create_zmlp_credential(client, wallet_credential, project_id, datasource_name):
    """Creates and returns ZMLP credentials based on a wallet credential object."""
    if not wallet_credential:
        return None
    credential_type = wallet_credential['type'].upper()

    # Validate the credential data.
    serializer_map = {'GCP': GcpCredentialSerializer,
                      'AWS': AwsCredentialSerializer,
                      'AZURE': AzureCredentialSerializer}
    serializer_map[credential_type](data=wallet_credential).is_valid(raise_exception=True)

    # Create a new credential and return its UUID.
    blob = wallet_credential.copy()
    del blob['type']
    if credential_type == 'GCP':
        blob = json.loads(wallet_credential['service_account_json_key'])
    payload = {'name': f'console - {datasource_name[:15]} - {uuid.uuid4()}',
               'type': credential_type,
               'blob': json.dumps(blob)}
    credential = client.post('/api/v1/credentials', payload)
    return credential['id']


@lru_cache(maxsize=128)
def get_module_name(module_id, client):
    """Gets a pipeline module name based on its ID."""
    response = client.get(f'/api/v1/pipeline-mods/{module_id}')
    return response['name']


class DataSourceViewSet(ZmlpListMixin,
                        ZmlpRetrieveMixin,
                        ZmlpDestroyMixin,
                        BaseProjectViewSet):
    """CRUD operations for ZMLP Data Sources.

When creating a data source 3 types of credentials can be passed; GCP, AWS, or AZURE.
Below are examples of all 3.

***GCP Credential:***

    "credentials": {
        "type": "GCP",
        "service_account_json_key": "<Contents of GCP json service key file>"
    }


***AWS Credential:***

    "credentials": {
        "type": "AWS",
        "aws_access_key_id": "sdlkmsoijes;kfjnskajnre",
        "aws_secret_access_key": "sdkjfipuenkjrfewrf"
    }


***Azure Credential:***

    "credentials": {
        "type": "AZURE",
        "connection_string": "DefaultEndpointsProtocol=http;AccountName=account;AccountKey=myKey;",
    }

"""
    serializer_class = DataSourceSerializer
    zmlp_root_api_path = '/api/v1/data-sources/'
    list_type = ListViewType.SEARCH

    @staticmethod
    def list_modifier(request, datasource):
        modules = datasource.get('modules')
        if modules:
            datasource['modules'] = [get_module_name(m, request.client) for m in
                                     modules]

    def create(self, request, project_pk):
        serializer = CreateDataSourceSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        app = request.app
        data = serializer.validated_data
        credential = create_zmlp_credential(request.client, data.get('credentials'),
                                            project_pk, data.get('name'))
        try:
            body = {'name': data['name'],
                    'uri': data['uri'],
                    'modules': data['modules'],
                    'credentials': [credential] if credential else [],
                    'fileTypes': data['fileTypes']}
            datasource = DataSource(request.client.post(self.zmlp_root_api_path, body=body))
        except BoonSdkDuplicateException:
            body = {'name': ['A Data Source with that name already exists.']}
            return Response(body, status=409)
        job = app.datasource.import_files(datasource)
        datasource._data['jobId'] = job.id
        serializer = self.get_serializer(data=datasource._data)
        validate_zmlp_data(serializer)
        return Response(serializer.validated_data, status=status.HTTP_201_CREATED)

    def update(self, request, project_pk, pk):
        self._zmlp_update(request, pk)
        datasource = request.app.datasource.get_datasource(pk)
        job = request.app.datasource.import_files(datasource)
        datasource._data['jobId'] = job.id
        serializer = self.get_serializer(data=datasource._data)
        validate_zmlp_data(serializer)
        return Response(serializer.validated_data)

    @action(detail=True, methods=['post', 'put'])
    def scan(self, request, project_pk, pk):
        path = os.path.join(self.zmlp_root_api_path, pk, '_import')
        response = request.client.post(path, {})
        return Response({'jobId': response['id']})
