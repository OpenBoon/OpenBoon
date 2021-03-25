import logging
import os

from boonsdk import app_from_env, BoonSdkException

logger = logging.getLogger(__name__)

__all__ = [
    "Frame",
    "ExpandFrame",
    "Context",
    "Processor",
    "Generator",
    "AssetProcessor",
    "Argument",
    "FatalProcessorException",
    "ProcessorException",
    "BoonEnv",
    "FileTypes",
    "ModelTrainer"
]


class FileTypes:
    """
    A class for storing the supported file types.
    """

    videos = frozenset(['mov', 'mp4', 'mpg', 'mpeg', 'm4v', 'webm', 'ogv', 'ogg', 'mxf', 'avi'])
    """A set of supported video file formats."""

    images = frozenset(["bmp", "cin", "dpx", "gif", "jpg",
                        "jpeg", "exr", "png", "psd", "rla", "tif", "tiff",
                        "dcm", "rla"])
    """A set of supported image file formats."""

    documents = frozenset(['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'vsd', 'vsdx'])
    """A set of supported document file formats."""

    all = videos.union(images).union(documents)
    """A set of all supported file formats."""


class UnsetArgumentValue:
    def __repr__(self):
        return self.__str__()

    def __str__(self):
        return "<Unset Value>"

    def __getitem__(self, k):
        return None

    def __len__(self):
        return 0


class Argument:
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
        self.settings = {}

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


class Frame:
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


class ExpandFrame:
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


class Context:
    """The Context class contains to a processors's runtime environment. This
    includes a reactor instance, args, and global vars
    """

    def __init__(self, reactor, args, settings=None):
        """
        Initialize a new context.

        Args:
            reactor (Reactor): A reactor instance.
            args (dict): A dict of ZPS script args
            settings (dict): A dict of global ZPS script args
        """
        self.reactor = reactor
        self.args = args or {}
        self.settings = settings or {}

    def get_arg(self, name, def_value=Argument.NOT_SET):
        """Get the value of the named arg.

        Args:
            name (str): The name of the arg.
            def_value (:obj:`mixed`): The default value of the arg is not set.

        Returns:
            :obj:`mixed`: The value of the arg or the def_value

        """
        return self.args.get(name, def_value)


class Processor:
    """The base class for all Processors.

    There are currently two types of processors:

    * Generators - create documents to process.
    * AssetBuilders - process assets created by generators.

    """

    file_types = FileTypes.all
    """An optional set of file types a subclass allows."""

    version = 1
    """The version of the processor, defaults to 1.  If you increment the version
     of your processor, it will not be skipped on a re-process operation."""

    namespace = None
    """The attribute namespace the processor controls, this is mainly informational."""

    use_threads = True
    """If True, the processor executes batches of assets in parallel."""

    fatal_errors = False
    """If True, all errors are fatal."""

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
                    raise FatalProcessorException(
                        msg % (arg_name, arg_expr, e))

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


class AssetProcessor(Processor):
    """
    Base class for AssetBuilder processors. An AssetProcessor is handed a Frame
    which contains the Asset being processed.
    """

    def preprocess(self, assets):
        """
        Run a pre-process on the assets. The assets can not be modified.

        Args:
            assets:

        Returns:

        """
        pass

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
            raise BoonSdkException("No reactor set on processor")
        return self.reactor.add_expand_frame(parent_frame, expand_frame,
                                             batch_size, force)


class ModelTrainer(AssetProcessor):
    """
    A base class for model training processors.
    """

    use_threads = False
    """Do not use threads"""

    file_types = None
    """Set file types to None"""

    def __init__(self):
        super(ModelTrainer, self).__init__()
        self.add_arg(Argument("model_id", "str", required=True))
        self.add_arg(Argument("post_action", "str", required=True))
        self.add_arg(Argument("tag", "str", required=True))
        self.app_model = None

    def load_app_model(self):
        self.logger.info("Fetching model {}".format(self.arg_value('model_id')))
        self.app_model = self.app.models.get_model(self.model_id)

    def process(self, frame):
        self.train()

    def train(self):
        """
        Subclasses should implement this method with the training logic.
        """
        pass

    @property
    def model_id(self):
        """The ID of the model to train."""
        return self.arg_value("model_id")

    @property
    def post_action(self):
        """The action to take after training is complete."""
        return self.arg_value("post_action")

    @property
    def tag(self):
        """The version tag of the model we're file we're training."""
        return self.arg_value("tag")


class BoonEnv:
    """
    Static methods for obtaining environment variables available when running
    within an analysis container.
    """

    @staticmethod
    def get_job_id():
        """
        Return the Boon AI Job id from the environment.

        Returns:
            str: The Boon AI task Id.
        """
        return os.environ.get("BOONAI_JOB_ID")

    @staticmethod
    def get_task_id():
        """
        Return the Boon AI Task id from the environment.

        Returns:
            str: The Boon AI task Id.
        """
        return os.environ.get("BOONAI_TASK_ID")

    @staticmethod
    def get_project_id():
        """
        Return the Boon AI project id from the environment.

        Returns:
            str: The Boon AI project Id.
        """
        return os.environ.get("BOONAI_PROJECT_ID")

    @staticmethod
    def get_datasource_id():
        """
        Return the Boon AI DataSource id from the environment.  The DataSource ID
        may or may not exist.

        Returns:
            str: The Boon AI DataSource Id or None
        """
        return os.environ.get("BOONAI_DATASOURCE_ID")

    @staticmethod
    def get_available_credentials_types():
        """
        Get a list of the available credentials types available to this job.

        Returns:
            list: list of credentials types.
        """
        return os.environ.get("BOONAI_CREDENTIALS_TYPES", "").split(",")


class ProcessorException(BoonSdkException):
    """
    The base class for processor exceptions.
    """
    pass


class FatalProcessorException(ProcessorException):
    """
    Thrown by a processor when it makes no sense to continue processing
    the asset due to an unrecoverable error.
    """
    pass
