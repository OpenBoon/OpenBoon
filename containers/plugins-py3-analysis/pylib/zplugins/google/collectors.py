import json
import os

from google.cloud import pubsub_v1, storage
from google.cloud.exceptions import NotFound

from zorroa.zclient import ZorroaJsonEncoder
from zorroa.zsdk import Collector, Argument


class PubSubCollector(Collector):
    """Collector that publishes the json representation of all completed assets to a
    GCP Pub/Sub topic. The data in the messages is a json encoded list of all documents.

    """
    def __init__(self, *args, **kwargs):
        super(PubSubCollector, self).__init__()
        self.add_arg(Argument('topic', type='str', default='zorroa-processing-output'))
        self.publisher = None
        self.topic_path = None

    def init(self):
        super(PubSubCollector, self).init()
        project = storage.Client().project
        self.publisher = pubsub_v1.PublisherClient()
        self.topic_path = self.publisher.topic_path(project, self.arg_value('topic'))
        try:
            self.publisher.get_topic(self.topic_path)
        except NotFound:
            self.publisher.create_topic(self.topic_path)

    def _get_data(self, collected):
        """Returns a json string to use as the data for the pub/sub message.

        Args:
            collected: List of Frames that were collected.

        Returns: Json string with all the information for assets in the frames collected.

        """
        assets = [frame.asset.for_json() for frame in collected]
        return json.dumps(assets, cls=ZorroaJsonEncoder).encode('utf-8')

    def collect(self, collected):
        data = self._get_data(collected)
        # TODO: Check data size and break into smaller chunks based on a size threshold.
        # Default pubsub message limit is 10mb.
        self.publisher.publish(self.topic_path, data=data,
                               job_id=os.environ.get("ZORROA_JOB_ID"),
                               task_id=os.environ.get("ZORROA_TASK_ID"),
                               organization_id=os.environ.get("ZORROA_ORGANIZATION_ID"))
        self.logger.info('Published %s assets to the %s pub/sub topic.' %
                         (len(collected), self.topic_path))
