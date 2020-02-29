export const jobErrorNonFatal = {
  id: '85f0d09b-0631-104b-a57e-ce490d54f27a',
  taskId: 'd2bb6ad3-54d0-11ea-9250-32240f207bfa',
  jobId: '85f0d094-0631-104b-a57e-ce490d54f27a',
  assetId: '5o7oiiw9WhA5ZkkOn_KZcMxValbwOBvn',
  path: 'gs://zorroa-dev-data/image/cat.gif',
  message:
    'ElasticsearchException[Elasticsearch exception [type=strict_dynamic_mapping_exception, reason=mapping set to strict, dynamic introduction of [checksum] within [metrics.pipeline] is not allowed]]',
  processor: 'unknown',
  fatal: false,
  analyst: 'not-implemented',
  phase: 'index',
  timeCreated: 1582306669906,
  jobName: 'Applying modules:  to gs://zorroa-dev-data',
  stackTrace: [
    {
      file: '/usr/local/lib/python3.7/dist-packages/zmlpcd/process.py',
      lineNumber: 143,
      className: 'new_processor_instance',
      methodName: 'instance = getattr(module, cls_name)()',
    },
  ],
}

export const jobErrorFatal = {
  id: '85f0d09c-0631-104b-a57e-ce490d54f27a',
  taskId: 'd2bb6ad3-54d0-11ea-9250-32240f207bfa',
  jobId: '85f0d094-0631-104b-a57e-ce490d54f27a',
  assetId: 'HfQ4xpvClTgRHFidgyXRcK01e6u239Lv',
  path: 'gs://zorroa-dev-data/image/frog.jpeg',
  message:
    'ElasticsearchException[Elasticsearch exception [type=strict_dynamic_mapping_exception, reason=mapping set to strict, dynamic introduction of [checksum] within [metrics.pipeline] is not allowed]]',
  processor: 'unknown',
  fatal: true,
  analyst: 'not-implemented',
  phase: 'index',
  timeCreated: 1582306669906,
  jobName: 'Applying modules:  to gs://zorroa-dev-data',
  stackTrace: [],
}
