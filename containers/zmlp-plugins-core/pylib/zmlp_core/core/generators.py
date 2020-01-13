from urllib.parse import urlparse

from zmlpsdk import Generator, Argument
from zmlpsdk.cloud import get_google_storage_client
from zmlp.asset import FileImport


class GcsBucketGenerator(Generator):
    """Simple generator that fetches all of the objects in a GCS bucket.

    To use this generator in production, google credentials must be mounted to the container.
    If no credentials are found it will attempt to use anonymous credentials.  To open
    a bucket to an Anonymous GCS user, give  "Storage Object Viewer" or
    "Storage Legacy Bucket Reader" to "allUsers".

    Args:
        uri (str): Address of a bucket in the form "gs://<BUCKET_NAME>".

    """

    def __init__(self):
        super(GcsBucketGenerator, self).__init__()
        self.add_arg(Argument('uri', 'str', required=True))

    def generate(self, consumer):
        uri = urlparse(self.arg_value('uri'))
        storage_client = get_google_storage_client()
        for blob in storage_client.list_blobs(uri.netloc, prefix=uri.path.lstrip("/")):
            if blob.name.endswith("/"):
                continue
            gsuri = "gs://{}/{}".format(uri.netloc, blob.name)
            consumer.accept(FileImport(gsuri))
