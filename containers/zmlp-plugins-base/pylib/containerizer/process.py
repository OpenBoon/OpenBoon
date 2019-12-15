import hashlib
import json
import logging
import sys
import time
import os

from pixml.asset import Asset
from pixml.analysis import Frame, Context, PixmlUnrecoverableProcessorException

logger = logging.getLogger(__name__)


class ProcessorExecutor(object):
    """
    Handles execution of a single Processor on a single data object.
    """

    def __init__(self, reactor):
        self.reactor = reactor
        self.processors = {}

    def execute_generator(self, request):
        logger.info('--Generating------------')
        logger.info(json.dumps(request, indent=4))
        logger.info('------------------------')

        ref = request["ref"]
        settings = request.get("settings", {})

        wrapper = self.get_processor_wrapper(ref)
        wrapper.generate(settings)

    def execute_processor(self, request):
        """
        Executes a single processor on a single data object and returns
        the object in a new state.

        Args:
            request (dict): An execution request

        Returns:
            The processed data object.
        """
        logger.info('--Processing------------')
        logger.info(json.dumps(request, indent=4))
        logger.info('------------------------')

        ref = request["ref"]
        obj = request.get("asset")
        frame = Frame(Asset(obj))

        wrapper = self.get_processor_wrapper(ref)
        wrapper.process(frame)
        return frame

    def teardown_processor(self, request):
        """
        Run the teardown for a give n
        Args:
            request (dict): a teardown request

        Returns:
            True if the processor was torn down.

        """
        ref = request["ref"]
        logger.info("tearing down processor='{}'".format(ref["className"]))

        key = self.get_processor_key(ref)
        wrapper = self.processors.get(key)
        if wrapper:
            wrapper.teardown()
            del self.processors[key]
            return True
        else:
            logger.warning("Failed to teardown processor, missing from cache {}".format(ref))
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
            wrapper.init()
            self.processors[key] = wrapper
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
            logger.warning("Failed to create new instance of {}, {}".format(ref["className"], e))
            self.reactor.error(None, ref.get("className"), e, True, "initialize", sys.exc_info()[2])
            self.reactor.emitter.write({
                "type": "hardfailure",
                "payload": {}
            })
            # Return an empty wrapper here so we can centralize the point
            # where the 'final' event is emitted.
            return None


class ProcessorWrapper(object):
    """
    Wraps a given Processor instance to provide:

    * Statistics
    * Events
    * Error handling

    """

    def __init__(self, instance, ref, reactor):
        self.instance = instance
        self.ref = ref
        self.reactor = reactor
        self.stats = {
            "processor": ref["className"],
            "image": ref["image"],
            "error_count": 0,
            "unrecoverable_error_count": 0,
            "process_count": 0,
            "total_time": 0
        }

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
            if self.instance:
                self.instance.generate(consumer)
                total_time = round(time.monotonic() - start_time, 2)
                self.increment_stat("generate_count")
                self.increment_stat("total_time", total_time)
            else:
                logger.warning("Generate warning, instance for '{}' does not exist."
                               .format(self.ref))
        except PixmlUnrecoverableProcessorException as upe:
            # Set the asset to be skipped for further processing
            # It will not be included in result
            self.increment_stat("unrecoverable_error_count")
            self.reactor.error(None, self["ref"]["className"],
                               upe, True, "execute", sys.exc_info()[2])
        except Exception as e:
            self.increment_stat("error_count")
            self.reactor.error(None, self.instance, e, False, "execute", sys.exc_info()[2])
        finally:
            consumer.check_expand(True)
            self.reactor.emitter.write({
                "type": "finished",
                "payload": {}
            })

    def process(self, frame):
        """
        Run the Processor instance on the given Frame.
        Args:
            frame (Frame):

        """
        start_time = time.monotonic()
        try:
            if self.instance and is_file_type_allowed(frame.asset, self.instance.file_types):
                self.instance.process(frame)
            else:
                logger.warning("Execute warning, instance for '{}' does not exist."
                               .format(self.ref))

            total_time = round(time.monotonic() - start_time, 2)
            self.increment_stat("process_count")
            self.increment_stat("total_time", total_time)

            # Check the expand queue.  A force check is done at teardown.
            self.reactor.check_expand()

        except PixmlUnrecoverableProcessorException as upe:
            # Set the asset to be skipped for further processing
            # It will not be included in result
            frame.skip = True
            self.increment_stat("unrecoverable_error_count")
            self.reactor.error(frame, self.ref,
                               upe, True, "execute", sys.exc_info()[2])
        except Exception as e:
            self.increment_stat("error_count")
            self.reactor.error(frame, self.ref, e, False, "execute", sys.exc_info()[2])
        finally:
            self.reactor.emitter.write({
                "type": "asset",
                "payload": {
                    "asset": frame.asset.for_json(),
                    "skip": frame.skip
                }
            })
            self.reactor.emitter.write({
                "type": "finished",
                "payload": {}
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
            self.reactor.emitter.write({"type": "stats", "payload": [self.stats]})
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
        val = self.stats.get(key, 0) + value
        self.stats[key] = val
        return val


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
        self.file_types = {ft.lower() for ft in settings.get("fileTypes", [])}
        self.batch_size = int(settings.get("batchSize", reactor.default_batch_size))
        self.frame_count = 0
        self.execute_count = 0
        self.expand_count = 0
        self.exit_status = 0
        self.expand = []

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
            logger.warning('Failed to parse extension for file: {}'.format(asset.uri, e))
            return False
    else:
        return True
