import itertools
import logging
import os
import threading
import traceback

logger = logging.getLogger(__name__)


class Reactor:
    """
    The Reactor is used to write cluster events like errors and expands
    back to a listening service like an Analyst.

    """
    # The default batch size if the Zps script does not provide one.
    default_batch_size = int(os.environ.get("BOONAI_ZPS_BATCH_SIZE", 50))

    # The number of stack trace elements to provide in error events.
    stack_trace_limit = 6

    def __init__(self, emitter, batch_size=None):
        """Create and return a Reactor instance.

        Args:
            emitter (object): An Emitter implementation has a single
                write() method which is used to write events back to whatever
                is collecting them, usually the Analyst.
            batch_size (int): The default expand batch size.
                This can be overridden by processors when check_expand() is
                    called.
        """
        self.emitter = emitter
        self.batch_size = batch_size or max(self.default_batch_size, 1)
        self.expand_frames = []
        self.expand_lock = threading.RLock()

    def add_expand_frame(self, parent_frame, expand_frame, batch_size=None, force=False):
        """Add an expand frame to the Reactor. If the expand frame buffer is
        full an Expand event will be emitted automatically.  Alternatively,
        you can pass in  a batch_size and force flag to customize the size
        of the Expand event.

        Args:
            parent_frame (:obj:`Frame`): the parent frame.  Used to copy
                attrs from parent to child.
            expand_frame (:obj:`ExpandFrame`): the ExpandFrame
            batch_size (:obj:`int`, optional): An optional batch size,
                otherwise uses default from ZPS script
            force (:obj:`bool`, optional): Optionally force the expand
                buffer to emit regardless of size.
        Returns:
            int: The number of Expand events generated.

        """
        with self.expand_lock:
            self.expand_frames.append((parent_frame, expand_frame))
            return self.check_expand(batch_size, force)

    def clear_expand_frames(self, parent_id=None):
        """Clear out expand frames buffer. This can be done by parent asset ID in
        the case that the parent Asset failed to process after adding children
        to the expand buffer.

        Args:
            parent_id (:obj:`str`, optional): optional parent ID for clearing a
                specific parent.

        """
        with self.expand_lock:
            # expand_frames is a list of tuple (parent frame, expand_frame)
            if parent_id:
                self.expand_frames = [expand for expand in self.expand_frames if
                                      expand[0].asset.id != parent_id]

            else:
                self.expand_frames[:] = []

    def check_expand(self, batch_size=None, force=False):
        """Check the expand buffer to see if it has met or exceeded the desired
        size.  Optionally force the expand buffer to be processed.

        Args:
            batch_size (int): The size to check for.  Defaults to None which
                pulls size from the running script.
            force (bool): Force emitting the expand buffer.

        Returns:
            int: The number of batches created.

        """
        with self.expand_lock:
            queue_size = len(self.expand_frames)
            if not queue_size:
                return 0

            batch_size = batch_size or self.batch_size

            # If force is not set, check if the expand buffer has met the batch
            # size
            if not force and queue_size < batch_size:
                return 0

            logger.info("Expanding, task queue at {}, batch_size={}".format(queue_size, batch_size))

            def grouper(n, iterable):
                it = iter(iterable)
                while True:
                    chunk = tuple(itertools.islice(it, n))
                    if not chunk:
                        return
                    yield chunk

            batch_count = 0
            for group in grouper(batch_size, self.expand_frames):
                batch_count += 1
                over = []
                # Note that, at the time of the expand the clip source
                # asset is likely not fully processed, so trying to do
                # a bunch of attr copying here isn't going to do what you
                # want.
                for parent_frame, expand_frame in group:
                    over.append(expand_frame.asset.for_json())

                self.expand(over)

            self.clear_expand_frames()
            return batch_count

    def expand(self, assets):
        """Emit an Expand event.  An Expand event will create a new task for the
        current job.

        Args:
            assets (list of dict): A list of assets to process.

        """
        self.write_event("expand", {"assets": assets})

    def error(self, frame, processor, exp, fatal, phase, exec_traceback=None):
        """Emit an Error

        Args:
            frame (:obj:`Frame`): The frame we had
            processor (:obj:`class`): The processor the error occurred on.
            exp (:obj:`Exception`): The exception that was thrown, or an error
                message.
            fatal (bool): If the error was fatal or not.
            phase (str): The phase at which the error occurred.
            exec_traceback (Traceback): An optional traceback from sys.exc_info

        """
        proc_name = None
        if isinstance(processor, str):
            proc_name = processor
        elif isinstance(processor, dict):
            proc_name = processor.get("className", "UnknownClass")
        elif processor:
            try:
                proc_name = processor.__class__.__name__
            except Exception as e:
                logger.warning("Failed to determine proc name from %s, %s" % (processor, e))

        if isinstance(exp, Exception):
            message = "%s: %s" % (exp.__class__.__name__, exp)
        else:
            message = str(exp)

        payload = {
            "processor": proc_name,
            "message": message,
            "fatal": fatal,
            "phase": phase or "execute"
        }
        if frame:
            payload["path"] = frame.asset.get_attr("source.path")
            payload["assetId"] = frame.asset.id

        # Convert the python stack trace to a server side StackTraceElement
        if exec_traceback:
            trace = traceback.extract_tb(exec_traceback)
            if len(trace) > self.stack_trace_limit:
                trace = trace[-self.stack_trace_limit:]

            stack_trace_for_payload = []
            for ste in trace:
                stack_trace_for_payload.append({
                    "file": ste[0],
                    "lineNumber": ste[1],
                    "className": ste[2],
                    "methodName": ste[3]
                })
            payload["stackTrace"] = stack_trace_for_payload

        self.write_event("error", payload)

    def performance_report(self, report):
        """
        Emit a performance report.

        Args:
            report: A JSON string containing processing time statistics
        """
        self.write_event("stats", report)

    def emit_status(self, text):
        """
        Emit and log the text as a new task status.

        Args:
            text (str): A task status.

        """
        logger.info("Task Status: {}".format(text))
        self.write_event("status", {"status": text})

    def progress(self, progress):
        """
        Emit and log the text as a new task status.

        Args:
            progress (int): The task progress.

        """
        logger.info("Task Process: {}%".format(progress))
        self.write_event("progress", {"progress": max(0, min(progress, 100))})

    def write_event(self, type, payload):
        """
        Write an event back to the analyst.

        Args:
            type (str): The type of event.
            payload: (dict): The event payload, empty dict for no payload

        """
        self.emitter.write({"type": type, "payload": payload})
