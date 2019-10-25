import os

from mock import patch

from zorroa.zsdk import Asset
from zorroa.zsdk.logs import setup_logger_format


@patch('zsdk.logs._add_stackdriver_log_handler')
def test_configure_stackdriver_logging(patch):
    LABELS = {}

    def add_stackdriver_mock(labels, level):
        # The LABELS dict is accessed in this manner to it reach out of the local scope.
        LABELS.clear()
        for k, v in labels.iteritems():
            LABELS[k] = v

    patch.side_effect = add_stackdriver_mock
    os.environ['STACKDRIVER_LOGGING'] = 'yes please'
    os.environ['ZORROA_TASK_ID'] = 'task-1'
    os.environ['ZORROA_JOB_ID'] = 'job-1'
    os.environ['ZORROA_ORGANIZATION_ID'] = 'org-1'

    setup_logger_format()
    expected_labels = {'analyst_task_id': 'task-1',
                       'analyst_job_id': 'job-1',
                       'analyst_organization_id': 'org-1'}
    assert LABELS == expected_labels

    asset = Asset('/not/real.jpg')
    setup_logger_format(asset=asset)
    expected_labels['analyst_asset_id'] = asset.id
    expected_labels['analyst_file_name'] = asset.source_path
    assert LABELS == expected_labels
