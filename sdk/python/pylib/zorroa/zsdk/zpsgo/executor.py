import json
import logging
import time
import sys
import os

from zorroa.client.http import ZorroaJsonEncoder
from .processors import DocumentGenerator
from zorroa.zsdk import Context, Reactor, Collector
from zorroa.zsdk.exception import UnrecoverableProcessorException

from zorroa.zsdk.logs import setup_logger_format, reset_logger_format

logger = logging.getLogger(__name__)

__all__ = [
    "ZpsExecutor",
    "is_frame_file_type_filtered"
]

"""
Default values for ZPS script settings.

A typical ZPS script may or may define various execution settings.  For any setting
that is not defined, a default value is pulled from the zps_script_defaults dict.
"""
zps_script_defaults = {
    "strict": False,
    "inline": False,
    "batchSize": None,
    "fileTypes": None
}


class ProcessorStatsCollector(object):
    """
    The ProcessorStatsCollector is a class for collecting performance/timing information
    of the various processors run by the ZpsExecutor during the interpretation of ZPS
    scripts.
    """
    def __init__(self):
        self.stats = dict()

    def __str__(self):
        """
        Get a string representation of the processor statistics.

        Returns:
            (string) This object's string representation.

        """
        return "ProcessorStats: " + str(self.stats)

    def add(self, processor_name, processing_time):
        """
        Add a timing entry.
        Args:
            processor_name: The processor's name.
            processing_time: The time consumed by the processor's process() method invocation.
        """
        timings = self.stats.get(processor_name, [])
        timings.append(processing_time)
        self.stats[processor_name] = timings

    def reset(self):
        """
        Reset the collector to empty state.
        """
        self.stats = dict()

    def summary(self):
        """
        Make a dictionary with the processor timing statistics collected.

        Returns:
            (dict) Dictionary of the processor statistics
        """
        result = list()
        for name, times in self.stats.items():
            entry = dict()
            entry["processor"] = name
            entry["count"] = len(times)
            entry["min"] = min(times)
            entry["max"] = max(times)
            entry["avg"] = sum(times) / entry["count"]
            result.append(entry)
        return result


class ZpsExeception(Exception):
    pass


class FrameConsumer(object):
    """
    The FrameConsumer class is passed into a generator to serve as
    a reference to the zps script execution framework.

    For each file the generator finds, it calls the accept() method with the
    frame.  If the file passes the defined file filters, then it is either held for expand or
    processed inline.

    Args:
        executor(ZpsExecutor) - the zps executor instance
        procs(list[Processor]) -  a list of processors to run for each frame.
        reactor(Reactor) - a reactor for talking back to the Archivist
        script(dict) - A ZPS script to execute.

    """

    def __init__(self, executor, procs, reactor, script):
        self.executor = executor
        self.procs = procs
        self.reactor = reactor
        self.frame_count = 0
        self.execute_count = 0
        self.expand_count = 0
        self.exit_status = 0
        self.expand = []

        # Settings and the default values should already be setup
        # at this point
        self.settings = script["settings"]

        # Log the settings for debugging purposes.
        logger.info("Script settings:")
        for k, v in self.settings.items():
            logger.info("script setting: %s=%s" % (k, v))

    def accept(self, frame):
        """
        Called by the generator once it finds a frame to emit.

        Args:
            frame(Document): The frame of data to filter for acceptance.
        """
        if is_frame_file_type_filtered(frame, self.settings["fileTypes"]):
            return
        self.frame_count += 1
        if self.settings["inline"]:
            self.execute_count += self.executor.process(
                frame, self.reactor, self.procs, self.settings["strict"])
        else:
            self.expand.append(frame)
            self.check_expand(False)

    def check_expand(self, force=False):
        """
        Checks to see if the current job should be expanded.

        Args:
            force(bool): force an expand even if the batchSize is not full.
        """
        waiting = len(self.expand)
        if waiting > 0 and (waiting >= self.reactor.batch_size or force):
            self.expand_count += 1
            script = {
                "over": [f.asset.for_json() for f in self.expand]
            }
            logger.info("#%d Expand %d frames into new task" % (self.expand_count, waiting))
            self.reactor.expand(script)
            self.expand = []


class ZpsExecutor(object):
    """
    ZpsExecutor is responsible for the interpretation of ZPS scripts.

    """
    def __init__(self):
        self.processor_stats = ProcessorStatsCollector()

    def run(self, script, script_args=None, teardown=True, reactor=None):

        # Get a reference to the script settings with defaults applied
        settings = apply_default_script_settings(script)
        # reset processor stats for this run of procesors
        self.processor_stats.reset()

        # The reactor handles cluster communication
        if not reactor:
            reactor = Reactor(self, script["settings"]["batchSize"])

        execute = None
        generators = None

        try:
            # Initialize full pipeline and generators.
            global_args = script.get('globalArgs', {})
            logger.info("Initializing processors")
            execute = self.initialize(reactor, script.get("execute"), script_args, global_args)

            logger.info("Initializing generators")
            generators = self.initialize(reactor, script.get("generate"), script_args, global_args)

            # If the script has not explicitly defined a fileTypes to override, then gather up
            # all file types from downstream processors and stuff those into settings. These
            # are used later on to filter at the generator level.
            if not settings.get("fileTypes"):
                file_types = execute["fileTypes"].union(generators["fileTypes"])
                settings["fileTypes"] = file_types
                logger.info("Auto-detected file type filtering %s" % settings["fileTypes"])

            # If all we have is a static document generator then
            # we don't care about filtering types or expanding.
            if script.get("over"):
                dgen = DocumentGenerator(script.get("over"))
                generators["processors"].append(dgen)
                if len(generators["processors"]) == 1:
                    settings["fileTypes"] = None
                    settings["inline"] = True

            consumer = FrameConsumer(self, execute, reactor, script)

            for generator in generators["processors"]:
                generator_name = generator.__class__.__name__
                logger.info("----[Executing generator: %s]----" % generator_name)
                generator.set_expression_values(None)
                stime = time.time()
                try:
                    with ProcessorEnvironment(generator.ref.get("env")):
                        generator.generate(consumer)
                        consumer.check_expand(True)
                    duration = time.time() - stime
                    logger.info("----[Completed generator: %s in %0.2fs]----"
                                % (generator_name, duration))
                except Exception as e:
                    # A broken generator kills the process.
                    logger.exception("Generator '%s' failed, unexpected exception %s",
                                     generator_name, e)
                    reactor.error(None, generator, e, False, "generate")
                    consumer.exit_status = 1
                    break
        finally:
            if teardown:
                logger.info("Tearing down generators")
                self.teardown(generators, reactor)
                logger.info("Tearing down processors")
                self.teardown(execute, reactor)

        # Issue a performance report for the various processors run.
        reactor.performance_report(self.processor_stats.summary())

        return consumer

    def process(self, frame, reactor, execute, strict=False):

        logger.info('------[Processing Asset %s - %s]------' %
                    (frame.asset.id, frame.asset.source_path))
        count = 0
        processed = True

        if frame.skip:
            return

        for proc in execute["processors"]:

            setup_logger_format(proc, frame.asset)

            logger.info('----[Executing processor: %s]----' % proc.__class__.__name__)
            if is_frame_file_type_filtered(frame, get_file_type_filter(proc)):
                continue

            if proc.ref.get("filters", []):
                filter_allow = False
                filter_drop = False
                for filt in proc.ref["filters"]:
                    if eval(filt["expr"], {"_doc": frame.asset}):
                        filter_allow = True
                        break
                    else:
                        if filt.get("drop", False):
                            filter_drop = True
                            break
                if filter_drop:
                    break
                if not filter_allow:
                    continue
            count += 1
            stime = time.time()
            try:
                with ProcessorEnvironment(proc.ref.get("env")):
                    proc.set_expression_values(frame)
                    proc.process(frame)

                etime = time.time()
                duration = etime - stime

                logger.info('----[Completed processor: %s in %0.2fs]----'
                            % (proc.__class__.__name__, duration))
                self.processor_stats.add(proc.full_class_name(), duration)

                # If we encounter a collector, that terminates the segment or the frame is skipped.
                if isinstance(proc, Collector) or frame.skip:
                    break

                if proc.execute:
                    count += self.process(frame, reactor, proc.execute, strict)
                    if frame.skip:
                        break

            except UnrecoverableProcessorException as e:
                logger.exception("UnrecoverableProcessorException caused by '%s' in '%s'"
                                 % (e, proc))
                processed = False
                reactor.error(frame, proc, e, True, "execute", sys.exc_info()[2])
                break
            except Exception as ex:
                if strict:
                    logger.warning("Not Recovering from '%s' on processor '%s', strict enabled" %
                                (ex, proc))
                    reactor.error(frame, proc, ex, True, "execute", sys.exc_info()[2])
                else:
                    logger.warning("Recovering from '%s' on processor '%s'" % (ex, proc))
                    reactor.error(frame, proc, ex, False, "execute", sys.exc_info()[2])

                logger.exception(str(ex))
                if strict:
                    processed = False
                    break

        reset_logger_format()

        # If the frame has been skipped or not processed
        # The the expand buffer is cleared, since we don't
        # add derived assets for skipped/broken frames.
        if frame.skip or not processed:
            reactor.clear_expand_frames(frame.asset.id)
            return count

        return count

    def teardown(self, processors, reactor):
        reactor.check_expand(force=True)

        if not processors:
            logger.warn("No processors to teardown")
            return

        logger.info("Running teardown on %d processors" % len(processors["processors"]))
        for proc in processors["processors"]:
            try:
                with ProcessorEnvironment(proc.ref.get("env")):
                    proc.teardown()
            except Exception as ex:
                logger.exception("Recovering from '%s' on processor '%s'" % (ex, proc))
                reactor.error(None, proc, ex, False, "teardown")

    def initialize(self, reactor, procs, script_args, global_args):
        if not procs:
            return {
                "processors": [],
                "reactor": reactor,
                "filters": [],
                "fileTypes": frozenset([])
            }

        # The list of processor instances.
        processors = []

        # A combined list of all file types being filtered by the processors
        # This is propagated out so files are skipped before the pipeline
        # even executes.  If this doesn't happen a ton of expand frames
        # get created with garbage files.
        file_types = []

        # TODO: gather up all the filters from any sub pipeline
        filters = []

        if isinstance(script_args, dict):
            global_args.update(script_args)

        index = -1
        for pref in procs:
            index += 1
            proc = self.new_processor(pref, reactor)
            args = pref.get("args", dict())
            proc.set_context(Context(reactor, args, global_args))

            with ProcessorEnvironment(proc.ref.get("env")):
                proc.init()

            processors.append(proc)

            # Add any file type filters to the segment list
            file_types.extend(get_file_type_filter(proc))

            logger.info("Initialized: %s" % pref["className"])
            if pref.get("execute"):
                child_segment = self.initialize(
                    reactor, pref.get("execute"), script_args, global_args)
                proc.execute = child_segment
                proc.execute_refs = pref.get("execute")

            if isinstance(proc, Collector):
                # TODO: aggregate sub execute filters into the collector.
                break

        return {
            "processors": processors,
            "reactor": reactor,
            "filters": filters,
            "fileTypes": frozenset(file_types)
        }

    def write(self, command):
        try:
            s = json.dumps(command, cls=ZorroaJsonEncoder)
            print("######## BEGIN ########\n%s\n######## END ##########" % s)
        except Exception as e:
            logger.exception("task failure, failed to serialize command %s, %s" % (command, e))
            raise UnrecoverableProcessorException("Failed to serialize reaction: %s" % e)

    def new_processor(self, ref, reactor):
        """
        Construct and return an instance of the processor described by
        the given processor reference dict.

        Args:
            ref (dict): A processor reference dict
            reactor (Reactor): A process reactor instance.

        Returns:
            Processor: an instance of a Processor class.

        """
        try:
            mod_name, cls_name = ref["className"].rsplit(".", 1)
            mod_name = str(mod_name)
            cls_name = str(cls_name)

            module = __import__(mod_name, globals(), locals(), [cls_name])
            instance = getattr(module, cls_name)()
            instance.ref = ref
        except Exception as e:
            reactor.error(None, ref.get("className"), e, True, "initialize", sys.exc_info()[2])
            raise UnrecoverableProcessorException("Failed to initialize processor '%s', %s"
                                                  % (ref.get("className"), e))
        return instance


def apply_default_script_settings(script):
    """
    Apply default settings to the script when some settings are
    not provided.  Returns the combined settings.

    Args:
        script: The script to pull settings from

    Returns: dict with defaults applied.

    """
    settings = script.get("settings", dict())
    defaults = dict(zps_script_defaults)
    defaults.update(settings)
    script["settings"] = defaults
    return defaults


def get_file_type_filter(proc):
    """
    Checks the arguments for a processor to determine if a fileTypes
    arg exists and return it if it does, otherwise return the value
    of the class level file_types property.

    Args:
        proc(Processor): the processor to check for the fileTypes arg

    Returns: list

    """
    arg_file_types = proc.arg_value("fileTypes")
    if arg_file_types:
        return arg_file_types
    elif proc.file_types:
        return proc.file_types
    return []


def is_frame_file_type_filtered(frame, file_types):
    """
    Determine if a given frame is filtered out by a set of file types.

    Args:
        frame(Frame): the frame to check
        file_types(set): the file typed filter to check against

    Returns: False if the frame is NOT filtered.
    """
    result = False
    if file_types:
        ext = frame.asset.get_attr("source.extension")
        if ext and ext.lower() not in [ft.lower() for ft in file_types]:
            logger.debug("Skipping filtered file type %s" % ext)
            result = True
    return result


class ProcessorEnvironment(object):
    """
    A context class which handles switching modifying the os.environ
    that a processor sees during init, process, and teardown.
    """
    def __init__(self, env):
        self.env = env
        if self.env:
            self.original = os.environ.copy()

    def __enter__(self):
        if self.env:
            for k, v in self.env.items():
                logger.debug("Setting env {}={}.".format(k, v))
                os.environ[k] = v
        return self

    def __exit__(self, ex_type, ex_value, tb):
        if self.env:
            logger.debug("Resetting env.")
            os.environ = self.original
