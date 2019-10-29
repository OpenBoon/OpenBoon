import sys
import json
import hashlib
import logging
import time

from zorroa.zsdk.processor import Frame, Context
from zorroa.zsdk.document.asset import Asset, Document
from zorroa.zsdk.exception import UnrecoverableProcessorException

logger = logging.getLogger(__name__)


class ProcessorExecutor(object):
    """
    Handles execution of a single Processor on a single data object.
    """
    def __init__(self, reactor):
        self.reactor = reactor
        self.processors = {}

    def execute_processor(self, request):
        """
        Executes a single processor on a single data object and returns
        the object in a new state.

        Args:
            request (dict): An execution request

        Returns:
            The processed data object.
        """
        ref = request["ref"]
        obj = request.get("object")
        frame = Frame(Asset.from_document(Document(obj)))

        logger.info("executing processor='{}' on asset={}'"
                    .format(ref["className"], frame.asset.id))

        wrapper = self.get_processor_wrapper(ref)
        wrapper.process(frame)
        return frame.asset.for_json()

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

    def process(self, frame):
        """
        Run the Processor instance on the given Frame.
        Args:
            frame (Frame):

        """
        start_time = time.monotonic()
        try:
            if self.instance:
                self.instance.process(frame)
            else:
                logger.warning("Execute warning, instance for '{}' does not exist.", self.ref)

            total_time = round(time.monotonic() - start_time, 2)
            self.increment_stat("process_count")
            self.increment_stat("total_time", total_time)
        except UnrecoverableProcessorException as upe:
            # Set the asset to be skipped for further processing
            # It will not be included in result
            frame.skip = True
            self.increment_stat("unrecoverable_error_count")
            self.reactor.error(frame, self["ref"]["className"],
                               upe, True, "execute", sys.exc_info()[2])
        except Exception as e:
            self.increment_stat("error_count")
            self.reactor.error(frame, self.instance, e, False, "execute", sys.exc_info()[2])
        finally:
            self.reactor.emitter.write({
                "type": "object",
                "payload": {
                    "object": frame.asset.for_json(),
                    "skip": frame.skip
                }
            })

    def teardown(self):
        """
        Run the teardown for the wrapped Processor instance.

        """
        if not self.instance:
            logger.warning("Teardown error, instance for '{}' does not exist.", self.ref)
            return
        try:
            self.instance.teardown()
            self.reactor.emitter.write({"type": "stats", "payload": {"stats": self.stats}})
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
        self.stats[key] += value
        return self.stats[key]

