const jobError = {
  id: '85f0d09b-0631-104b-a57e-ce490d54f27a',
  taskId: 'd2bb6ad3-54d0-11ea-9250-32240f207bfa',
  jobId: '85f0d094-0631-104b-a57e-ce490d54f27a',
  assetId: '5o7oiiw9WhA5ZkkOn_KZcMxValbwOBvn',
  path: 'gs://zorroa-dev-data/image/cat.gif',
  message:
    'ElasticsearchException[Elasticsearch exception [type=strict_dynamic_mapping_exception, reason=mapping set to strict, dynamic introduction of [checksum] within [metrics.pipeline] is not allowed]]',
  processor: 'unknown',
  fatal: true,
  analyst: 'not-implemented',
  phase: 'index',
  timeCreated: 1582306669906,
  jobName: 'Applying modules:  to gs://zorroa-dev-data',
  stackTrace: [
    {
      file: '/opt/app-root/src/analyst/pylib/zplugins/image/importers.py',
      lineNumber: 43,
      className: '_process',
      methodName: 'path = Path(asset.get_local_source_path())',
    },
    {
      file: '/opt/app-root/lib/python2.7/site-packages/zsdk/document/asset.py',
      lineNumber: 220,
      className: 'get_local_source_path',
      methodName:
        'local_cache_location = self.__source_handler.store_local_cache()',
    },
    {
      file:
        '/opt/app-root/lib/python2.7/site-packages/zsdk/document/sourcehandler.py',
      lineNumber: 139,
      className: 'store_local_cache',
      methodName: 'self.blob.download_to_filename(local_path)',
    },
    {
      file:
        '/opt/app-root/lib/python2.7/site-packages/google/cloud/storage/blob.py',
      lineNumber: 759,
      className: 'download_to_filename',
      methodName: 'raw_download=raw_download,',
    },
    {
      file:
        '/opt/app-root/lib/python2.7/site-packages/google/cloud/storage/blob.py',
      lineNumber: 722,
      className: 'download_to_file',
      methodName: '_raise_from_invalid_response(exc)',
    },
    {
      file:
        '/opt/app-root/lib/python2.7/site-packages/google/cloud/storage/blob.py',
      lineNumber: 2156,
      className: '_raise_from_invalid_response',
      methodName:
        'raise exceptions.from_http_status(response.status_code, message, response=response)',
    },
  ],
}

export default jobError
