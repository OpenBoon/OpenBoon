import os
import subprocess

from zorroa.zsdk import Generator, Argument, Asset, Frame


class GcsBucketGenerator(Generator):
    """Simple generator that gets all of the objects in a GCS bucket and creates an asset.

    Args:
        bucket (str): Address of a bucket in the form "gs://<BUCKET_NAME>".

    Important:
    - The analysts must be configured with a service account that has access to the bucket.
    - Best suited for testing purposes to easily ingest from a bucket. This is not
    full-featured enough yet to be a good candidate for production use.

    """
    def __init__(self):
        super(GcsBucketGenerator, self).__init__()
        self.add_arg(Argument('bucket', 'str', required=True))

    def generate(self, consumer):
        output = subprocess.check_output(['gsutil', 'ls',
                                          os.path.join(self.arg_value('bucket'), '**')])
        paths = output.split('\n')
        for path in paths:
            if path:
                asset = Asset(path)
                consumer.accept(Frame(asset))
