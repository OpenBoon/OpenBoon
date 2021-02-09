import datetime
import hashlib
import json
import logging
import os
import sys
import threading
import time
from queue import Queue

import requests
import sentry_sdk

from zmlp import Asset
from zmlpsdk import Frame, Context, ZmlpFatalProcessorException, ZmlpEnv
from .logs import AssetLogger

sentry_sdk.init('https://8d2c5bb15a2241349c05f8915e10a888@o280392.ingest.sentry.io/5600983',
                environment=os.environ.get('ENVIRONMENT', 'local-dev'))
logger = logging.getLogger(__name__)


class ProcessorExecutor(object):
    """
    Handles execution of a single Processor on a single data object.
    """

    def __init__(self, reactor):
        self.reactor = reactor
        self.processors = {}
        self.queue = WorkQueue(int(os.environ.get("ANALYST_THREADS", "4")))

    def execute_generator(self, request):
        if logger.isEnabledFor(logging.DEBUG):
            logger.debug('--Generating------------')
            logger.debug(json.dumps(request, indent=4))
            logger.debug('------------------------')

        ref = request["ref"]
        settings = request.get("settings", {})

        logger.info('Executing generator=\'{}\''.format(ref['className']))
        wrapper = self.get_processor_wrapper(ref)
        wrapper.generate(settings)

    def execute_preprocess(self, request):
        assets = request.get("assets")
        ref = request["ref"]
        wrapper = self.get_processor_wrapper(ref)
        wrapper.preprocess([Asset(a) for a in assets])

    def execute_processor(self, request):
        """
        Executes a single processor on a single data object and returns
        the object in a new state.

        Args:
            request (dict): An execution request

        Returns:
            The processed data object.
        """
        if logger.isEnabledFor(logging.DEBUG):
            logger.debug('--Processing------------')
            logger.debug(json.dumps(request, indent=4))
            logger.debug('------------------------')

        ref = request["ref"]
        assets = request.get("assets")

        wrapper = self.get_processor_wrapper(ref)

        # Multi-thread
        if wrapper.instance:

            if wrapper.instance.use_threads:
                for asset in assets:
                    self.queue.add_asset(wrapper, asset)
                # Wait on the thread pool to be empty.
                self.queue.join()

            # Single thread
            else:
                for asset in assets:
                    self.queue.process_asset(wrapper, asset)
        else:
            logger.warning(
                "The processor {} has no instance, the class was not found".format(
                    wrapper.class_name))

        return assets

    def teardown_processor(self, request):
        """
        Run the teardown for a give n
        Args:
            request (dict): a teardown request

        Returns:
            True if the processor was torn down.

        """
        ref = request.get("ref")
        if not ref:
            self.warning("Invalid teardown request, missing a processor ref.")
            return

        logger.info("tearing down processor='{}'".format(ref["className"]))

        key = self.get_processor_key(ref)
        wrapper = self.processors.get(key)
        if wrapper:
            wrapper.teardown()
            del self.processors[key]
            return True
        else:
            self.warning("Failed to teardown processor, missing from cache {}".format(ref))
        return False

    def get_processor_key(self, ref):
        """
        Given a processor reference, calculate a unique hash.

        Args:
            ref (dict): A Processor reference.

        Returns:
            (str): a hash in hex format.

        """
        sha = hashlib.sha256()
        sha.update(json.dumps(ref, sort_keys=True, default=str).encode("utf-8"))
        return sha.hexdigest()

    def get_processor_wrapper(self, ref):
        """
        Given a Processor reference, return an initialized ProcessorWrapper

        Args:
            ref (dict): A Processor reference.

        Returns:
            (ProcessorWrapper): an initialized wrapper.
        """
        # Determine if we already have a processor instance.
        # Utilize the existing instance.
        key = self.get_processor_key(ref)
        if key not in self.processors:
            wrapper = ProcessorWrapper(self.new_processor_instance(ref), ref, self.reactor)
            self.processors[key] = wrapper

            try:
                wrapper.init()
            except Exception as e:
                # set the instance to None
                wrapper.instance = None

                msg = "Failed to init() instance of {}, unexpected: " \
                      "'{}' exception".format(ref["className"], e)
                logger.exception(msg)

                # If its a standard module it can't fail so its a hard error.
                # Otherwise we get tons of random errors and its hard to figure
                # out what is going on..
                if ref.get("module") == "standard":
                    self.reactor.write_event("hardfailure", {"message": msg + "(standard)"})
                else:
                    self.reactor.error(None, ref["className"], e, False, "init")
        else:
            wrapper = self.processors[key]

        return wrapper

    def new_processor_instance(self, ref):
        """
        Construct and return an instance of the processor described by
        the given processor reference dict.

        Args:
            ref (dict): A processor reference dict

        Returns:
            Processor: an instance of a Processor.

        """
        try:
            mod_name, cls_name = ref["className"].rsplit(".", 1)
            mod_name = str(mod_name)
            cls_name = str(cls_name)
            module = __import__(mod_name, globals(), locals(), [cls_name])
            instance = getattr(module, cls_name)()
            logger.debug("Created new instance of {}".format(ref["className"]))
            return instance
        except Exception as e:
            msg = "Failed to create new instance of {}, {}".format(ref["className"], e)
            logger.exception(msg)
            self.reactor.error(None, ref.get("className"), e, True, "initialize", sys.exc_info()[2])

            # If this is a standard module then this is a hard failure
            # kill the task.
            if ref.get("module") == "standard":
                self.reactor.write_event("hardfailure", {"message": msg})

            # Return an empty wrapper here so we can centralize the point
            # where the 'final' event is emitted.
            return None

    def warning(self, msg):
        """
        Log and emit a warning message.

        Args:
            msg (str): The warning message.

        """
        logger.warning(msg)
        self.reactor.write_event("warning", {"message": msg})


class ProcessorWrapper(object):
    """
    Wraps a given Processor instance to provide:

    * Statistics
    * Events
    * Error handling

    """

    def __init__(self, instance, ref, reactor):
        self.instance = instance
        self.ref = ref or {}
        self.reactor = reactor
        self.stats = {
            "processor": ref["className"],
            "image": ref["image"],
            "error_count": 0,
            "unrecoverable_error_count": 0,
            "process_count": 0,
            "total_time": 0
        }
        self.stat_lock = threading.RLock()

    @property
    def class_name(self):
        return self.ref["className"]

    @property
    def image_name(self):
        return self.ref["image"]

    def init(self):
        """
        Initialize the Processor instance by setting the execution Context
        and calling the init() method.

        """
        if self.instance:
            self.instance.set_context(Context(self.reactor,
                                              self.ref.get("args") or {}, {}))
            self.instance.init()

    def generate(self, settings):
        consumer = AssetConsumer(self.reactor, settings)
        start_time = time.monotonic()
        try:
            # If there are no file types then a bunch of garbage can get
            # imported, like .ds_store, etc.
            if not consumer.file_types:
                raise ValueError("No file types were supplied in job settings property")

            if self.instance:
                self.instance.generate(consumer)
                total_time = round(time.monotonic() - start_time, 2)
                self.increment_stat("generate_count")
                self.increment_stat("total_time", total_time)
            else:
                logger.warning("Generate warning, instance for '{}' does not exist."
                               .format(self.ref))

        except ZmlpFatalProcessorException as upe:
            self.increment_stat("unrecoverable_error_count")
            self.reactor.error(None, self.ref["className"],
                               upe, True, "execute", sys.exc_info()[2])
        except Exception as e:
            if self.instance.fatal_errors:
                self.increment_stat("unrecoverable_error_count")
            else:
                self.increment_stat("error_count")
            self.reactor.error(None, self.instance, e, self.instance.fatal_errors,
                               "generate", sys.exc_info()[2])
        finally:
            consumer.check_expand(True)
            self.reactor.write_event("finished", {})

    def preprocess(self, assets):
        start_time = time.monotonic()
        try:
            if self.instance:
                self.instance.preprocess(assets)
                total_time = round(time.monotonic() - start_time, 2)
                self.instance.logger.info("completed preprocess in {0:.2f}".format(total_time))
            else:
                logger.warning(
                    "The processor {} has no instance, the class was not found".format(
                        self.class_name))
        except Exception as e:
            # Preprocess is fatal.
            self.increment_stat("unrecoverable_error_count")
            self.reactor.error(None, self.ref, e,
                               True, "preprocess", sys.exc_info()[2])
        finally:
            # Always show metrics even if it was skipped because otherwise
            # the pipeline checksums don't work.
            self.reactor.write_event("preprocess", {})

    def process(self, frame):
        """
        Run the Processor instance on the given Frame.
        Args:
            frame (Frame):

        """
        start_time = time.monotonic()
        error = None
        total_time = 0
        processed = False

        try:
            # There is a finally clause down below that still handles
            # emitting the asset back to the Analyst on these
            # early return methods.

            if not self.instance:
                logger.warning("Execute warning, instance for '{}' does not exist."
                               .format(self.ref))
                return

            if self.is_already_processed(frame.asset):
                logger.debug("The asset {} is already processed".format(frame.asset.id))
                return

            if self.instance.file_types:
                if not is_file_type_allowed(frame.asset, self.instance.file_types):
                    # No need to log, this is normal.
                    return

            self.instance.logger.info("started processor")

            retval = self.instance.process(frame)
            # a -1 means the processor was skipped internally.
            processed = retval != -1
            if processed:
                self._record_analysis_metric(frame.asset)

            total_time = round(time.monotonic() - start_time, 2)
            self.increment_stat("process_count")
            self.increment_stat("total_time", total_time)

            # Check the expand queue.  A force check is done at teardown.
            self.reactor.check_expand()

        except ZmlpFatalProcessorException as upe:
            # Set the asset to be skipped for further processing
            # It will not be included in result
            frame.skip = True
            error = "fatal"
            self.increment_stat("unrecoverable_error_count")
            self.reactor.error(frame, self.ref,
                               upe, True, "execute", sys.exc_info()[2])
        except Exception as e:
            if self.instance.fatal_errors:
                error = "fatal"
                frame.skip = True
                self.increment_stat("unrecoverable_error_count")
            else:
                error = "warning"
                self.increment_stat("error_count")
            self.reactor.error(frame, self.ref, e,
                               self.instance.fatal_errors, "execute", sys.exc_info()[2])
        finally:
            # Always show metrics even if it was skipped because otherwise
            # the pipeline checksums don't work.
            logger.info("completed processor in {0:.2f}".format(total_time))
            self.apply_metrics(frame.asset, processed, total_time, error)
            self.reactor.write_event("asset", {
                "asset": frame.asset.for_json(),
                "skip": frame.skip
            })

    def teardown(self):
        """
        Run the teardown for the wrapped Processor instance.

        """
        if not self.instance:
            logger.warning("Teardown error, instance for '{}' does not exist.".format(self.ref))
            return
        try:
            self.instance.teardown()
            # When the processor tears down then force an expand check.
            self.reactor.check_expand(force=True)
            self.reactor.write_event("stats", [self.stats])
        except Exception as e:
            self.reactor.error(None, self.instance, e, False, "teardown", sys.exc_info()[2])

    def increment_stat(self, key, value=1):
        """
        Increment a stats counter by given value

        Args:
            key (str): name of stats key
            value (mixed): A value to increment by.

        Returns:
            (mixed): The new value
        """
        with self.stat_lock:
            val = self.stats.get(key, 0) + value
            self.stats[key] = val
            return val

    def apply_metrics(self, asset, processed, exec_time, error):
        """
        Apply execution metrics to the given asset.

        Args:
            asset (Asset): The asset
            processed (bool): True if the asset was actually handled by the processor.
            exec_time (float): The time the processor executed.
            error (str): A type of error, fatal or warning.

        """
        if not self.instance:
            return
        metrics = asset.get_attr("metrics.pipeline")
        if not metrics:
            metrics = []
            asset.set_attr("metrics.pipeline", metrics)

        # We're assuming that processors are unique here.
        # Which isn't always or technically the case, but we
        # could move that direction using group processors.
        def find(lst, value):
            for i, dic in enumerate(lst):
                if dic.get("processor") == value:
                    return i
            return -1

        class_name = self.ref["className"]
        metric_idx = find(metrics, class_name)
        if metric_idx == -1:
            metric = {
                "processor": class_name,
                "module": self.ref.get("module"),
                "executionTime": 0
            }
            metrics.append(metric)
        else:
            metric = metrics[metric_idx]

        # The checksum needs to be there even if its not processed
        # or else a zero checksums would signal reprocessing.
        metric["checksum"] = self.ref.get("checksum", 0)

        # Only processed processors get a date and a positive executionTime
        if processed:
            metric["executionTime"] = exec_time
            metric["executionDate"] = datetime.datetime.utcnow().isoformat()

        if error:
            metric["error"] = error

    def is_already_processed(self, asset):
        """
        Check if the asset has already been processed by this processor.  If
        the _force arg is set, then this function always returns false.

        Args:
            asset (Asset): The asset.
        Returns:
            bool True if the asset was processed by the processor
        """
        # If force is set at all, to anything, then its not already processed
        if self.ref.get("force"):
            return False

        metrics = asset.get_attr("metrics.pipeline")
        if not metrics:
            return False
        else:
            # Check if the class and processor are the same and no error exists
            value = ((m["processor"] == self.ref["className"]
                      and m["checksum"] == self.ref.get("checksum"))
                     and not m.get("error") for m in metrics)
            return any(value)

    def _record_analysis_metric(self, asset):
        """Helper to make the call to record billing metrics.

        Builds the required body to track asset, project, module, and image/video data
        for billing purposes. If the request fails for any reason, the failure is logged
        and execution continues

        Args:
            asset (:obj:`Asset`): The asset to register a billing metric for.
            module_name (:obj:`str`): The module namespace to record on the billing metric.

        """
        billing_service = os.environ.get('ZMLP_BILLING_METRICS_SERVICE', 'http://metrics')
        url = f'{billing_service}/api/v1/apicalls'
        # Abbreviate the string path
        source_path = asset.get_attr('source.path', default='')
        if len(source_path) > 255:
            # Include starting ellipses as an indicator, favor end of path
            source_path = '...' + source_path[len(source_path)-252:]
        image_count, video_minutes = self._get_count_and_minutes(asset)
        service = self.ref['module']
        body = {
            'project': ZmlpEnv.get_project_id(),
            'service': service,
            'asset_id': asset.id,
            'asset_path': source_path,
            'image_count': image_count,
            'video_minutes': video_minutes,
        }
        sentry_sdk.set_context('billing_metric', body)
        try:
            response = requests.post(url, json=body)
        except requests.exceptions.ConnectionError as e:
            msg = ('Unable to register billing metrics, could not connect to metrics service.')
            logger.warning(msg)
            sentry_sdk.capture_message(msg)
            sentry_sdk.capture_exception(e)
            msg = f'Metric missed: {body}'
            logger.warning(msg)

        if not response.ok:
            duplicate_msg = 'The fields service, asset_id, project must make a unique set.'
            if duplicate_msg in response.json().get('non_field_errors'):
                logger.info(f'Duplicate metric skipped for {asset.id}: {service}')
            else:
                msg = (f'Unable to register billing metrics. {response.status_code}: '
                       f'{response.reason}')
                logger.warning(msg)
                sentry_sdk.capture_message(msg)
                msg = f'Metric missed: {body}'
                logger.warning(msg)

    def _get_count_and_minutes(self, asset):
        """Helper to return total images and number of video minutes for an asset.

        Determines if the asset is a picture or video, and returns the image count or
        the total number of video minutes for the asset.

        Args:
            asset (:obj:`Asset`): The asset to find it's count or video minutes.

        Returns:
            (:obj:`tuple`): A two tuple of the number of images, and the number of video
                minutes.
        """
        media_type = asset.get_attr('media.type')
        if media_type == 'video':
            return (0, asset.get_attr('media.length'))

        # Assume it's an image or document then
        return (1, 0.0)


class AssetConsumer(object):
    """
    The AssetConsumer handles expand tasks created by generators. The reason
    to use AssetConsumer instead of just expanding directly from the Reactor
    is that the file types need to be filtered.

    For each file the generator finds, it calls the accept() method with the
    frame.  If the file passes the defined file filters, then it is either
    held for expand or processed inline.

    """
    def __init__(self, reactor, settings):
        """
        Create a new AssetConsumer instance.

        Args:
            reactor (Reactor):  a reactor for talking back to the Archivist
            settings(dict):  A dict of ZPS script settings.

        """
        self.reactor = reactor
        self.file_types = frozenset([ft.lower() for ft in settings.get("fileTypes", [])])
        self.batch_size = int(settings.get("batchSize", reactor.default_batch_size))
        self.frame_count = 0
        self.execute_count = 0
        self.expand_count = 0
        self.exit_status = 0
        self.expand = []

        logger.info("File types filters: {}".format(self.file_types))

    def accept(self, asset):
        """
        Called by the generator once it finds a frame to emit.

        Args:
            asset (Asset): The asset to consume.

        """
        if not is_file_type_allowed(asset, self.file_types):
            return False
        self.expand.append(asset)
        self.check_expand()
        return True

    def check_expand(self, force=False):
        """
        Checks to see if the current job should be expanded.

        Args:
            force(bool): force an expand even if the batchSize is not full.

        """
        waiting = len(self.expand)
        if waiting > 0 and (waiting >= self.batch_size or force):
            assets = [asset.for_json() for asset in self.expand]
            self.expand_count += 1

            logger.info("#%d Expand %d frames into new task" % (self.expand_count, waiting))
            self.reactor.expand(assets)
            self.expand = []


def is_file_type_allowed(asset, file_types):
    """
    Determine if a given frame is filtered out by a set of file types.

    Args:
        asset (Asset): the frame to check
        file_types (list of string): the file types and/or mimetypes filter to check against

    Returns:
        True if the file is allowed.

    """
    if file_types:
        try:
            _, ext = os.path.splitext(asset.uri)
            ext = ext[1:].lower()
            return ext in file_types
        except Exception as e:
            message = 'Failed to parse extension for file: {}. Unexpected: {}'.format(asset.uri, e)
            logger.warning(message)
            return False
    else:
        # Have to return true here for processors with no type filters.
        return True


class WorkQueue(Queue):
    """
    The WorkQueue is a basic thread pool that works on a queue
    of assets.
    """

    def __init__(self, num_workers):
        """
        Create a new WorkQueue instance with the given number of threads.

        Args:
            num_workers (int): The number of worker threads to spawn.
        """
        Queue.__init__(self)
        self.num_workers = num_workers
        self.__start_workers()

    def __start_workers(self):
        """
        Start the worker threads.
        """
        for i in range(self.num_workers):
            t = threading.Thread(target=self.worker)
            t.daemon = True
            t.start()

    def add_asset(self, wrapper, asset):
        """
        Add an asset to the processing queue.

        Args:
            wrapper (ProcessorWrapper): A ProceessorWrapper instance.
            asset (dict): An asset dictionary.
        """
        self.put([wrapper, asset])

    def worker(self):
        """
        The worker thread entry point function.
        """
        while True:
            # All threads block on get() until something appears in the queue.
            wrapper, asset = self.get()
            try:
                self.process_asset(wrapper, asset)
            finally:
                self.task_done()

    def process_asset(self, wrapper, asset):
        """
        Processes a given asset using the given processor wrapper.

        Args:
            wrapper (ProcessorWrapper): The processor wrapper to execute.
            asset (dict): The asset dictionary.
        """
        frame = Frame(Asset(asset))
        # This has to be done in the thread for
        # the logger to pick up the thread local value.
        AssetLogger.set_asset_id(frame.asset.id)
        try:
            wrapper.process(frame)
        finally:
            AssetLogger.clear_asset_id()
