const taskErrors = {
  count: 6,
  next: null,
  previous: null,
  results: [
    {
      id: '916c86bc-74b9-1519-b065-d2f0132bc0c8',
      taskId: 'b81f47e9-7382-1519-b88a-d2f0132bc0c8',
      jobId: '223fd17d-7028-1519-94a8-d2f0132bc0c8',
      assetId: 'dc145915-a33e-515a-a4e5-7ecd4b5fd1ba',
      path: 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/multipage/',
      message:
        "AttributeError: 'NoneType' object has no attribute 'startswith'",
      processor: 'HSVSimilarityProcessor',
      fatal: false,
      analyst: 'https://10.0.4.3:5000',
      phase: 'execute',
      timeCreated: 1579220726713,
      stackTrace: [
        {
          file:
            '/opt/app-root/lib/python2.7/site-packages/zsdk/zpsgo/executor.py',
          lineNumber: 284,
          className: 'process',
          methodName: 'proc.process(frame)',
        },
        {
          file: '/opt/app-root/lib/python2.7/site-packages/zsdk/processor.py',
          lineNumber: 746,
          className: 'process',
          methodName: 'self._process(frame)',
        },
        {
          file: '/opt/app-root/src/analyst/pylib/zplugins/image/processors.py',
          lineNumber: 27,
          className: '_process',
          methodName: "f = Path(asset.get_thumbnail_path()).open('rb')",
        },
        {
          file:
            '/opt/app-root/lib/python2.7/site-packages/zsdk/document/asset.py',
          lineNumber: 304,
          className: 'get_thumbnail_path',
          methodName:
            'if not path and mimetypes.guess_type(self.source_path)[0].startswith(',
        },
      ],
    },
    {
      id: '916c86bb-74b9-1519-b065-d2f0132bc0c8',
      taskId: 'b81f47e9-7382-1519-b88a-d2f0132bc0c8',
      jobId: '223fd17d-7028-1519-94a8-d2f0132bc0c8',
      assetId: 'dc145915-a33e-515a-a4e5-7ecd4b5fd1ba',
      path: 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/multipage/',
      message:
        "NotFound: 404 GET https://storage.googleapis.com/download/storage/v1/b/zorroa-deploy-testdata/o/zorroa-cypress-testdata%2Fmultipage?alt=media: (u'Request failed with status code', 404, u'Expected one of', 200, 206)",
      processor: 'DuplicateDetectionProcessor',
      fatal: true,
      analyst: 'https://10.0.4.3:5000',
      phase: 'execute',
      timeCreated: 1579220726642,
      stackTrace: [
        {
          file:
            '/opt/app-root/src/analyst/pylib/zplugins/duplicates/processors.py',
          lineNumber: 61,
          className: '_set_binary_match_hash',
          methodName: 'path = asset.get_local_source_path()',
        },
        {
          file:
            '/opt/app-root/lib/python2.7/site-packages/zsdk/document/asset.py',
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
    },
    {
      id: '916c86ba-74b9-1519-b065-d2f0132bc0c8',
      taskId: 'b81f47e9-7382-1519-b88a-d2f0132bc0c8',
      jobId: '223fd17d-7028-1519-94a8-d2f0132bc0c8',
      assetId: 'dc145915-a33e-515a-a4e5-7ecd4b5fd1ba',
      path: 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/multipage/',
      message:
        "AttributeError: 'NoneType' object has no attribute 'startswith'",
      processor: 'ResNetSimilarityProcessor',
      fatal: false,
      analyst: 'https://10.0.4.3:5000',
      phase: 'execute',
      timeCreated: 1579220726558,
      stackTrace: [
        {
          file:
            '/opt/app-root/lib/python2.7/site-packages/zsdk/zpsgo/executor.py',
          lineNumber: 284,
          className: 'process',
          methodName: 'proc.process(frame)',
        },
        {
          file: '/opt/app-root/lib/python2.7/site-packages/zsdk/processor.py',
          lineNumber: 746,
          className: 'process',
          methodName: 'self._process(frame)',
        },
        {
          file: '/opt/app-root/src/analyst/pylib/zplugins/mxnet/processors.py',
          lineNumber: 105,
          className: '_process',
          methodName:
            "p_path = asset.get_attr('tmp.proxy_source_image') or asset.get_thumbnail_path()",
        },
        {
          file:
            '/opt/app-root/lib/python2.7/site-packages/zsdk/document/asset.py',
          lineNumber: 304,
          className: 'get_thumbnail_path',
          methodName:
            'if not path and mimetypes.guess_type(self.source_path)[0].startswith(',
        },
      ],
    },
    {
      id: '916c86b9-74b9-1519-b065-d2f0132bc0c8',
      taskId: 'b81f47e9-7382-1519-b88a-d2f0132bc0c8',
      jobId: '223fd17d-7028-1519-94a8-d2f0132bc0c8',
      assetId: 'dc145915-a33e-515a-a4e5-7ecd4b5fd1ba',
      path: 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/multipage/',
      message:
        "AttributeError: 'NoneType' object has no attribute 'startswith'",
      processor: 'ResNetClassifyProcessor',
      fatal: true,
      analyst: 'https://10.0.4.3:5000',
      phase: 'execute',
      timeCreated: 1579220726533,
      stackTrace: [
        {
          file:
            '/opt/app-root/lib/python2.7/site-packages/zsdk/zpsgo/executor.py',
          lineNumber: 284,
          className: 'process',
          methodName: 'proc.process(frame)',
        },
        {
          file: '/opt/app-root/lib/python2.7/site-packages/zsdk/processor.py',
          lineNumber: 746,
          className: 'process',
          methodName: 'self._process(frame)',
        },
        {
          file: '/opt/app-root/src/analyst/pylib/zplugins/mxnet/processors.py',
          lineNumber: 40,
          className: '_process',
          methodName: 'p_path = asset.get_thumbnail_path()',
        },
        {
          file:
            '/opt/app-root/lib/python2.7/site-packages/zsdk/document/asset.py',
          lineNumber: 304,
          className: 'get_thumbnail_path',
          methodName:
            'if not path and mimetypes.guess_type(self.source_path)[0].startswith(',
        },
      ],
    },
    {
      id: '916c86b8-74b9-1519-b065-d2f0132bc0c8',
      taskId: 'b81f47e9-7382-1519-b88a-d2f0132bc0c8',
      jobId: '223fd17d-7028-1519-94a8-d2f0132bc0c8',
      assetId: 'dc145915-a33e-515a-a4e5-7ecd4b5fd1ba',
      path: 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/multipage/',
      message:
        "AttributeError: 'NoneType' object has no attribute 'startswith'",
      processor: 'FaceRecognitionProcessor',
      fatal: false,
      analyst: 'https://10.0.4.3:5000',
      phase: 'execute',
      timeCreated: 1579220726455,
      stackTrace: [
        {
          file:
            '/opt/app-root/lib/python2.7/site-packages/zsdk/zpsgo/executor.py',
          lineNumber: 284,
          className: 'process',
          methodName: 'proc.process(frame)',
        },
        {
          file: '/opt/app-root/lib/python2.7/site-packages/zsdk/processor.py',
          lineNumber: 746,
          className: 'process',
          methodName: 'self._process(frame)',
        },
        {
          file: '/opt/app-root/src/analyst/pylib/zplugins/face/processors.py',
          lineNumber: 41,
          className: '_process',
          methodName: 'p_path = asset.get_thumbnail_path()',
        },
        {
          file:
            '/opt/app-root/lib/python2.7/site-packages/zsdk/document/asset.py',
          lineNumber: 304,
          className: 'get_thumbnail_path',
          methodName:
            'if not path and mimetypes.guess_type(self.source_path)[0].startswith(',
        },
      ],
    },
    {
      id: '916c86b7-74b9-1519-b065-d2f0132bc0c8',
      taskId: 'b81f47e9-7382-1519-b88a-d2f0132bc0c8',
      jobId: '223fd17d-7028-1519-94a8-d2f0132bc0c8',
      assetId: 'dc145915-a33e-515a-a4e5-7ecd4b5fd1ba',
      path: 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/multipage/',
      message:
        "NotFound: 404 GET https://storage.googleapis.com/download/storage/v1/b/zorroa-deploy-testdata/o/zorroa-cypress-testdata%2Fmultipage?alt=media: (u'Request failed with status code', 404, u'Expected one of', 200, 206)",
      processor: 'ImageImporter',
      fatal: false,
      analyst: 'https://10.0.4.3:5000',
      phase: 'execute',
      timeCreated: 1579220726423,
      stackTrace: [
        {
          file: '/opt/app-root/src/analyst/pylib/zplugins/image/importers.py',
          lineNumber: 43,
          className: '_process',
          methodName: 'path = Path(asset.get_local_source_path())',
        },
        {
          file:
            '/opt/app-root/lib/python2.7/site-packages/zsdk/document/asset.py',
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
    },
  ],
}

export default taskErrors
