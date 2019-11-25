import errno
import itertools
import logging
import os
import stat
import traceback
from shutil import copyfile

from ..app import app_from_env
from ..exception import PixmlException
from ..util import import_and_instantiate, as_collection

logger = logging.getLogger(__name__)

__all__ = [
    "Frame",
    "ExpandFrame",
    "Context",
    "Processor",
    "Generator",
    "AssetBuilder",
    "Argument",
    "Reactor",
    "ProcessorHelper",
    "PixmlUnrecoverableProcessorException",
    "PixmlException",
    "PixmlProcessorException"
]


class UnsetArgumentValue(object):
    def __repr__(self):
        return self.__str__()

    def __str__(self):
        return "<Unset Value>"

    def __getitem__(self, k):
        return None

    def __len__(self):
        return 0


class Argument(object):
    """Describes an argument to a processor.

    Args:
        name (str): The name of the argument.
        type (str): The type of the argument: string, bool, int, float, struct,
            helper
        label (str): A nice label for the argument.  If one is not supplied,
            one is generated.
        default (mixed): The default value, defaults to None.
        required (bool): True if the argument is required. Defaults to false.
        toolTip (str): A useful description of what the argument does or
            controls.
        options: (:obj:`list` of :obj:`mixed'): A list of valid values the
            argument may have.
        regex (str): A regex which can be used to validate string values.
    """

    NOT_SET = UnsetArgumentValue()

    def __init__(self, name, type, label=None, default=None, required=False,
                 toolTip=None, options=None, regex=None):
        self.name = name
        self.label = label
        self.type = type
        self.default = default
        self.required = required
        self.tooltip = toolTip
        self.value = Argument.NOT_SET
        self.options = options
        self.regex = regex

        self.args = {}

    def add_arg(self, *args):
        for arg in args:
            self.args[arg.name] = arg
        return self

    def arg_value(self, name):
        return self.args[name]

    def __str__(self):
        return "<Argument name='%s' type='%s' value='%s'/>" % \
               (self.name, self.type, self.value)


class Frame(object):
    """Frames are used to pass data between processors.

    Attributes:
        skip(bool): If set to True at any time, the Frame will be skipped and
            the Asset will not appear in the DB.
    """
    def __init__(self, asset):
        """
       Construct a new Frame.

        Args:
            asset(:obj:`Asset`): The Asset the frame is wrapping.
        """
        self.asset = asset
        self.skip = False


class ExpandFrame(object):
    """When an Asset is broken down into child assets (pages, clips), the children
    are emitted as ExpandFrames which end up becoming new tasks.
    """
    def __init__(self, asset, copy_attrs=None):
        """
        Construct a new ExpandFrame.

        Args:
            asset (:obj:`Asset`): The new Asset to process.
            copy_attrs (:obj:`list` of :obj:`str'): A list of additional attrs
                to copy
        """
        self.asset = asset
        self.copy_attrs = copy_attrs


class Reactor(object):
    """
    The Reactor is used to write cluster events like errors and expands
    back to a listening service like an Analyst.

    """

    """
    The default batch size if the Zps script does not provide one.
    """
    default_batch_size = int(os.environ.get("ZORROA_ZPS_BATCH_SIZE", 50))

    """
    The number of stack trace elements to provide in error events.
    """
    stack_trace_limit = 6

    def __init__(self, emitter, batch_size=None):
        """Create and return a Reactor instance.

        Args:
            executor (:obj:`Executor`): An Executor implementation has a single
                write() method which is used to write events back to whatever
                is collecting them, usually the Analyst.
            batch_size (int): The default expand batch size.
                This can be overridden by processors when check_expand() is
                    called.
        """
        self.emitter = emitter
        self.batch_size = batch_size or max(self.default_batch_size, 1)
        self.expand_frames = []

    def add_expand_frame(self, parent_frame, expand_frame, batch_size=None,
                         force=False):
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
        if not len(self.expand_frames):
            return 0

        batch_size = batch_size or self.batch_size

        # If force is not set, check if the expand buffer has met the batch
        # size
        if not force and len(self.expand_frames) < batch_size:
            return 0

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
            for parent_frame, group_frame in group:

                # Copy metadata from the parent frame if necessary.  This is
                # set in frame.copy_attrs
                if group_frame.copy_attrs:
                    for attr in group_frame.copy_attrs:
                        logger.info("copy field='%s' to derived assetId='%s'" %
                                    (attr, parent_frame.asset.id))
                        group_frame.asset.set_attr(attr,
                                                   parent_frame.asset.get_attr(
                                                       attr))

                # Check for a list of attrs in the tmp namespace and copy them
                # down into each child.
                copy_attrs = parent_frame.asset.get_attr(
                    "tmp.copy_attrs_to_clip")
                if copy_attrs:
                    # handle the case where tmp.copy_attrs_to_clip may be a
                    # string
                    for attr in as_collection(copy_attrs):
                        logger.info("copy field='%s' to derived assetId='%s'" %
                                    (attr, parent_frame.asset.id))
                        group_frame.asset.set_attr(attr,
                                                   parent_frame.asset.get_attr(
                                                       attr))

                over.append(group_frame.asset.for_json())

            script = {"over": over}
            self.expand(script)

        self.clear_expand_frames()
        return batch_count

    def expand(self, script):
        """Emit an Expand event.  An Expand event will create a new task for the
        current job.

        Args:
            script (:obj:`dict`): A ZPS script structure.

        """
        self.emitter.write({"type": "expand", "payload": script})

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
            "phase": phase
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

        self.emitter.write({"type": "error", "payload": payload})

    def performance_report(self, report):
        """
        Emit a performance report.

        Args:
            report: A JSON string containing processing time statistics
        """
        self.emitter.write({"type": "stats", "payload": report})


class Context(object):
    """The Context class contains to a processors's runtime environment. This
    includes a reactor instance, args, and global vars
    """
    def __init__(self, reactor, args, global_args=None):
        """
        Initialize a new context.

        Args:
            reactor (:obj:`Reactor`): A reactor instance.
            args (:obj:`dict`): A dict of ZPS script args
            global_args (:obj:`dict`): A dict of global ZPS script args
        """
        self.reactor = reactor
        self.args = args or {}
        self.global_args = global_args or {}

    def get_arg(self, name, def_value=Argument.NOT_SET):
        """Get the value of the named arg.

        Args:
            name (str): The name of the arg.
            def_value (:obj:`mixed`): The default value of the arg is not set.

        Returns:
            :obj:`mixed`: The value of the arg or the def_value

        """
        return self.args.get(name, def_value)


class ProcessorException(Exception):
    """The base Exception for all Exceptions thrown by processors."""
    pass


class Processor(object):
    """The base class for all Processors.

    There are currently three types of processors:

    * Generators - create documents to process.
    * AssetBuilders - process assets created by generators.

    Attributes:
        file_types(list) - An optional set of file types a subclass allows.

    """

    file_types = None

    def __init__(self):
        self.execute = []
        self.filters = []
        self.context = None
        self.args = {}
        self.ref = {}
        self.execute_refs = []
        self.reactor = None
        self.expressions = {}
        self.app = app_from_env()
        self.logger = logging.getLogger(self.__class__.__name__)

    def full_class_name(self):
        c = self.__class__.__mro__[0]
        name = c.__module__ + "." + c.__name__
        return name

    def add_arg(self, arg):
        """Adds a predefined Argument to the Processor. Arguments should be added
        in the constructor.  Arguments will show up in the web interface as
        documentation for the Processor.

        Args:
            arg (:obj:`Argument`): Add a new Argument definition.

        Returns:
            :obj:`mixed`: Return the current instance (self)

        """
        self.args[arg.name] = arg
        return self

    def arg_value(self, name):
        """Return the value of a predefined argument.

        If no value is set, fall back to global args. If there is no global
        arg, then return the default value.

        Args:
            name (str): The name of the argument.

        Returns:
            :obj:`mixed`: The value of the argument.
        """
        try:
            return self.args[name].value
        except KeyError:
            return self.context.args.get(name)

    def arg(self, name):
        """Return the Argument with the given name.

        Arguments are setup in the constructor.

        Args:
            name (str): Name of the Argument.

        Returns:
            :obj:`Argument`: The Argument

        """
        return self.args.get(name)

    def set_context(self, context):
        """Set the context for the processor.

        Once the context is set, all arg values are set and the processor is

        ready for initialization.

        Args:
            context (:obj:`Argument`): The Processor run context.

        """
        self.context = context
        self.reactor = context.reactor
        self.__set_arg_values()

    def set_expression_values(self, frame):
        """Applies the frame to any python expressions set on this processor."""
        # The frame can be null in the case of a generator.
        if frame:
            ctx = {
                "_ctx": self.context,
                "_map": frame.asset.document,
                "_doc": frame.asset,
                "_frame": frame
            }
        else:
            ctx = {
                "_ctx": self.context
            }
        for arg_name, arg_expr in self.expressions.items():
            arg, expr = arg_expr
            try:
                arg.value = eval(expr["_expr_"], ctx)
            except Exception as e:
                if not expr.get("ignore_error"):
                    msg = "Failed to parse expression for arg '%s' : '%s', " \
                          "unexpected: %s"
                    raise PixmlUnrecoverableProcessorException(
                        msg % (arg_name, arg_expr, e))

    def get_model_path(self, rel_path, debug=False):
        """Returns the local drive location of a model file.

        Example: full_path = get_model_file_path('/mxnet/resnet-152')
        """

        def resolve_paths(rel_path):
            model_file_remote_top = '/tmp/zorroa-local/models'
            model_file_local_top = '/tmp/zorroa-local/models'
            return (os.path.join(model_file_remote_top, rel_path),
                    os.path.join(model_file_local_top, rel_path))

        def process_file(rel_path, remote_stat):
            if debug:
                self.logger.info("MODEL: Processing file: {}".format(rel_path))
            remote_path, local_path = resolve_paths(rel_path)

            # See if there's a local file.
            do_copy = False
            try:
                local_stat = os.stat(local_path)
            except Exception:
                do_copy = True

            # If there is a local file, see if it's out of date.
            if not do_copy:
                if remote_stat.st_mtime > local_stat.st_mtime:
                    do_copy = True

            # Do we need to copy?
            if do_copy:
                if debug:
                    self.logger.info(
                        "MODEL: Copying: {} -> {}".format(remote_path,
                                                          local_path))
                copyfile(remote_path, local_path)
            else:
                if debug:
                    self.logger.info(
                        "MODEL: Not necessary to copy: {} -> {}".format(
                            remote_path, local_path))

        def process_dir(rel_path):
            if debug:
                self.logger.info(
                    "MODEL: Processing directory: {}".format(rel_path))
            remote_path, local_path = resolve_paths(rel_path)

            # Make sure the directory exists locally
            try:
                os.makedirs(local_path)
            except OSError as exc:
                if exc.errno == errno.EEXIST and os.path.isdir(local_path):
                    pass
                else:
                    raise

            items = os.listdir(remote_path)
            for item in items:
                process_item(os.path.join(rel_path, item))

        def process_item(rel_path):
            if debug:
                self.logger.info("MODEL: Processing item: {}".format(rel_path))
            remote_path, local_path = resolve_paths(rel_path)

            try:
                remote_stat = os.stat(remote_path)
            except Exception:
                errmsg = "Can't find remote model path: {}".format(remote_path)
                raise ProcessorException(errmsg)

            if stat.S_ISREG(remote_stat.st_mode):
                process_file(rel_path, remote_stat)
            elif stat.S_ISDIR(remote_stat.st_mode):
                process_dir(rel_path)
            else:
                errmsg = "{} must refer to a file or directory".format(
                    rel_path)
                raise ProcessorException(errmsg)

            return local_path

        # Check to see if model caching functionality is disabled
        if os.environ.get('ZORROA_DISABLE_LOCAL_MODEL_CACHE'):
            remote_path, local_path = resolve_paths(rel_path)
            return remote_path

        return process_item(rel_path)

    def teardown(self):
        """Teardown is run automatically by the execution engine before a batch
        process is shut down.

        This method should be implemented by subclasses to free resources that
        require freeing, be it memory or temp file cleanup.
        """
        pass

    def init(self):
        """Init is run automatically by the execution engine before processing
        begins.

        This method should be implemented by subclasses to initialize class
        members that would normally go in the constructor.
        """
        pass

    def __set_arg_values(self):
        for arg in list(self.args.values()):
            value = self.context.get_arg(arg.name, Argument.NOT_SET)
            self.__walk_field(arg, value)

    def __walk_field(self, arg, value, parent_struct=None):
        # If value == Argument.NOT_SET, then it is not contained
        # in the passed in arguments.

        if value == Argument.NOT_SET:
            def_val = arg.default
            # If the default value is something, it might need further
            # processing depending on the type.
            if arg.type == "file":
                self.__handle_model_file(arg, def_val)
            else:
                value = def_val

        # if a parent struct is set, update the parent struct
        # with the new value as well.
        if parent_struct is not None:
            parent_struct[arg.name] = value

        if isinstance(value, dict):
            if "_expr_" in value:
                self.expressions[arg.name] = (arg, value)
                # arg.value = eval(value["_expr_"], {"_ctx": self.context})
        else:
            arg.value = value

        if arg.type == "struct":
            # Check if we have child fields, it he case where the type
            # is a list, dict, or set.
            for child_arg in list(arg.args.values()):
                self.__walk_field(child_arg,
                                  value.get(child_arg.name, Argument.NOT_SET),
                                  value)
            arg.value = arg.args

        if arg.type == 'dict':
            arg.value = value

        if arg.type == 'helper':
            arg.value = value
            if 'className' not in value:
                self.logger.warning('A helper value must contain a className')

        elif arg.type in ("list", "set"):
            # If a list has a single type, its a list of structs.
            # Each value, is a struct.
            try:
                child_arg = list(arg.args.values())[0]
                if arg.value:
                    for v in arg.value:
                        self.__walk_field(child_arg, v)
            except Exception:
                pass

    def instantiate_helper(self, helper_data):
        """Dynamically imports a ProcessorHelper subclass and instantiates it.

        The helper_data argument must be a dictionary matching the structure
        below. The "class" entry should be a dot path to the class for
        importing. The "kwargs" entry should be a dictionary of keyword
        arguments to instantiate the class with.

        Example helper_data:
            .. code-block:: json

                {
                    'class': 'dot.path.to.import.Class',
                    'kwargs': {'foo': 'bar'}
                }

        Args:
            helper_data (dict): Dictionary describing the ProcessorHelper.

        Returns:
            object: Object described by the helper data.
        """
        dot_path = helper_data['className']
        kwargs = helper_data.get('args', {})
        return import_and_instantiate(dot_path, self, **kwargs)


class Generator(Processor):
    """
    Base class for Generators.  Generators are responsible for provisioning Assets.
    """
    def __init__(self):
        super(Generator, self).__init__()

    def generate(self, consumer):
        """To emit data into the processing pipeline, call accept() on the
        consumer instance.

        The generate function is intended to be implemented by subclasses.

        :param consumer: a consumer which is passed in by the execution engine.
        :type consumer: class
        """
        pass


class AssetBuilder(Processor):
    """
    Base class for AssetBuilder processors. An AssetBuilder is handed a Frame
    which contains the Asset being processed.
    """
    def process(self, frame):
        """Process the given frame.

        Process is called by the execution engine once for every frame
        that has been generated. The process function
        calls the _process function that is intended to be implemented by
        subclasses.

        Args:
            frame (:obj:`Frame`): the frame to be processed

        """
        raise NotImplementedError

    def expand(self, parent_frame, expand_frame, batch_size=None, force=False):
        """Add an expand frame to the Reactor.

        If the expand frame buffer is full an Expand event will be emitted
        automatically. Alternatively, you can pass in a batch_size and force flag to
        customize the size of the Expand event.

        Args:
            parent_frame(:obj:`Frame`): the parent frame.  Used to copy attrs
                from parent to child.
            expand_frame(:obj:`ExpandFrame`): the ExpandFrame
            batch_size(:obj:`int`, optional): An optional batch size, otherwise
                uses default from ZPS script
            force(:obj:`bool`, optional): Optionally force the expand buffer to
                emit regardless of size.
        Returns:
            (int): The number of Expand events generated.
        """
        if not self.reactor:
            raise PixmlException("No reactor set on processor")
        return self.reactor.add_expand_frame(parent_frame, expand_frame,
                                             batch_size, force)


class ProcessorHelper(object):
    """Abstract Helper class used for swapping out different pieces of
    functionality in a Processor. All concrete ProcessorHelper classes should
    inherit from this class.  These classes are designed to dynamically
    imported and instantiated using the DocumentProcessor.instantiate_helper
    method. This provides an easy way to plugin custom functionality to
    existing processors.

    Args:
        processor (Processor): Processor that is being helped.

    """
    def __init__(self, processor):
        self.processor = processor

    @property
    def logger(self):
        return self.processor.logger


class PixmlProcessorException(PixmlException):
    """
    The base class for processor exceptions.
    """
    pass


class PixmlUnrecoverableProcessorException(ProcessorException):
    """
    Thrown by a processor when it makes no sense to continue processing
    the asseet due to an unrecoverable error.
    """
    pass
