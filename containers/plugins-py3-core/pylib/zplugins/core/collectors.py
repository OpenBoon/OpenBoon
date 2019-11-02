import logging
import os
import pprint
import tempfile
from zipfile import ZipFile

from pathlib2 import Path

from zorroa.zsdk.exception import UnrecoverableProcessorException
from zorroa.zsdk.processor import Collector, Argument
from zorroa.zclient import get_zclient

logger = logging.getLogger(__name__)


class ExpandCollector(Collector):
    """A collector which takes collected frames and a sub execute pipeline and
    submits it back to the execution engine to be executed as a separate process.

    Args:
        name (str): The name of the new task.

    """
    toolTips = {
        'name': 'Name of the task to create.'
    }

    def __init__(self):
        super(ExpandCollector, self).__init__()
        self.add_arg(Argument("name", "string", default="Expand Task",
                              toolTip=self.toolTips['name']))

    def collect(self, frames):
        if not self.frames:
            return

        script = {
            "execute": self.execute_refs,
            "frames": [{"document": f.asset.for_json()} for f in frames]
        }
        self.reactor.expand(script)


class ImportCollector(Collector):
    """Collect import files and register them with the archivist.

    Note: client setup is handled via ENV variables, ZORROA_TASK_ID and ZORROA_JOB_ID,
    which are set by the server so it can increment counters on the job and task when
    the assets are indexed.
    """
    def __init__(self):
        super(ImportCollector, self).__init__()
        self.client = None

    def init(self):
        self.client = get_zclient()

    def collect(self, frames):
        if not frames:
            return
        docs = []
        for frame in frames:
            docs.append(frame.asset.for_json())
        result = self.client.post("/api/v1/assets/_index", {
            "sources": docs,
            "taskId": os.environ.get("ZORROA_TASK_ID"),
            "jobId": os.environ.get("ZORROA_JOB_ID")})
        logger.debug("Archivist Indexing Results: \n%s", pprint.pformat(result))
