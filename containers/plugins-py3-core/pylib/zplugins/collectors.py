import logging
import os
import pprint
import tempfile
from zipfile import ZipFile

from pathlib2 import Path

from archivist import get_export
from zsdk.exception import UnrecoverableProcessorException

from zsdk.processor import Collector, Argument
from zsdk.util import get_export_root_dir

from archivist.client import Client

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
        self.client = Client()

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


class ExportCollector(Collector):
    """Collect export files and register them with the archivist."""

    def __init__(self):
        super(ExportCollector, self).__init__()
        self.paths_to_add = set()
        self.export_root_dir = get_export_root_dir()

    def init(self):
        """
        The ExportCollector init is responsible for making the output directory.

        Returns: None

        """
        self.export_root_dir.mkdir(parents=True, exist_ok=True)

    def teardown(self):
        """
        Create and register the exported zip file.  Checks to see if files were exported.
        If the output directory is empty, throws a UnrecoverableProcessorException.

        Returns: None

        """
        super(ExportCollector, self).teardown()
        export_root_dir = get_export_root_dir()
        export = get_export(self._get_export_id())

        files = os.listdir(str(export_root_dir))
        if not files:
            raise UnrecoverableProcessorException("No files were exported")

        with tempfile.NamedTemporaryFile(suffix="_%s.zip" % export.id) as tmp_file:
            with ZipFile(tmp_file, "w", allowZip64=True) as zip_file:
                for file_name in os.listdir(str(export_root_dir)):
                    path = export_root_dir.joinpath(file_name)
                    logger.info("Adding export to zip: %s" % path)
                    zip_file.write(str(path), str(Path(export.name, path.name)))
            self._add_ofs_file(export, tmp_file.name)

    def _add_ofs_file(self, export, path):
        """Store the given file to ofs and inform archivist of the export."""
        path = Path(path)
        extension = path.suffix.strip('.')
        export_file_name = '%s.%s' % (export.name, extension)
        object_file = self.ofs.prepare('job', export, "exported/%s" % export_file_name)
        object_file.store(str(path))
        export.add_file(object_file.id, export_file_name)

    def _get_export_id(self):
        """Returns the unique id for the export associated with this piepline."""
        return self.context.global_args['exportArgs']['exportId']
